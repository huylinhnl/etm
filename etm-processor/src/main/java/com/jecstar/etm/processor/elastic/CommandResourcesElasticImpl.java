package com.jecstar.etm.processor.elastic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.domain.Endpoint;
import com.jecstar.etm.domain.EndpointHandlingTimeComparator;
import com.jecstar.etm.processor.TelemetryCommand.CommandType;
import com.jecstar.etm.processor.core.CommandResources;
import com.jecstar.etm.processor.core.persisting.TelemetryEventPersister;
import com.jecstar.etm.processor.core.persisting.elastic.AbstractElasticTelemetryEventPersister;
import com.jecstar.etm.processor.core.persisting.elastic.BusinessTelemetryEventPersister;
import com.jecstar.etm.processor.core.persisting.elastic.HttpTelemetryEventPersister;
import com.jecstar.etm.processor.core.persisting.elastic.LogTelemetryEventPersister;
import com.jecstar.etm.processor.core.persisting.elastic.MessagingTelemetryEventPersister;
import com.jecstar.etm.processor.core.persisting.elastic.SqlTelemetryEventPersister;
import com.jecstar.etm.server.core.configuration.ConfigurationChangeListener;
import com.jecstar.etm.server.core.configuration.ConfigurationChangedEvent;
import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.EndpointConfiguration;
import com.jecstar.etm.server.core.domain.converter.json.EndpointConfigurationConverterJsonImpl;
import com.jecstar.etm.server.core.enhancers.DefaultTelemetryEventEnhancer;
import com.jecstar.etm.server.core.util.LruCache;

public class CommandResourcesElasticImpl implements CommandResources, ConfigurationChangeListener {

	private static final long ENDPOINT_CACHE_VALIDITY = 60_000;

	private final Client elasticClient;
	private final EtmConfiguration etmConfiguration;
	private final BulkProcessorListener bulkProcessorListener;
	
	private final EndpointHandlingTimeComparator endpointComparater = new EndpointHandlingTimeComparator();
	private final EndpointConfigurationConverterJsonImpl endpointConfigurationConverter = new EndpointConfigurationConverterJsonImpl();
	
	
	@SuppressWarnings("rawtypes")
	private Map<CommandType, TelemetryEventPersister> persisters = new HashMap<>();
	
	private BulkProcessor bulkProcessor;

	private Map<String, EndpointConfiguration> endpointCache = new LruCache<>(1000);
	
	public CommandResourcesElasticImpl(final Client elasticClient, final EtmConfiguration etmConfiguration, final MetricRegistry metricRegistry) {
		this.elasticClient = elasticClient;
		this.etmConfiguration = etmConfiguration;
		this.bulkProcessorListener = new BulkProcessorListener(metricRegistry);
		this.bulkProcessor = createBulkProcessor();
		this.etmConfiguration.addConfigurationChangeListener(this);
		
		this.persisters.put(CommandType.BUSINESS_EVENT, new BusinessTelemetryEventPersister(this.bulkProcessor, etmConfiguration));
		this.persisters.put(CommandType.HTTP_EVENT, new HttpTelemetryEventPersister(this.bulkProcessor, etmConfiguration));
		this.persisters.put(CommandType.LOG_EVENT, new LogTelemetryEventPersister(this.bulkProcessor, etmConfiguration));
		this.persisters.put(CommandType.MESSAGING_EVENT, new MessagingTelemetryEventPersister(this.bulkProcessor, etmConfiguration));
		this.persisters.put(CommandType.SQL_EVENT, new SqlTelemetryEventPersister(this.bulkProcessor, etmConfiguration));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getPersister(CommandType commandType) {
		return (T) persisters.get(commandType);
	}

	@Override
	public void loadEndpointConfig(List<Endpoint> endpoints, EndpointConfiguration endpointConfiguration) {
		endpointConfiguration.initialize();
		endpoints.sort(this.endpointComparater);
		for (Endpoint endpoint : endpoints) {
			if (endpoint.name != null) {
				mergeEndpointConfigs(endpointConfiguration, retrieveEndpoint(endpoint.name));
			}
		}
		mergeEndpointConfigs(endpointConfiguration, retrieveEndpoint(ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_ENDPOINT_DEFAULT));
	}
	
	private EndpointConfiguration retrieveEndpoint(String endpointName) {
		EndpointConfiguration cachedItem = endpointCache.get(endpointName);
		if (cachedItem != null) {
			if (System.currentTimeMillis() - cachedItem.retrievalTimestamp > ENDPOINT_CACHE_VALIDITY) {
				this.endpointCache.remove(endpointName);
			} else {
				return cachedItem;
			}
		}
		GetResponse getResponse = this.elasticClient.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_ENDPOINT, endpointName)
			.setFetchSource(true)
			.get();
		if (getResponse.isExists() && !getResponse.isSourceEmpty()) {
			EndpointConfiguration loadedConfig = this.endpointConfigurationConverter.read(getResponse.getSourceAsMap());
			loadedConfig.retrievalTimestamp = System.currentTimeMillis();
			this.endpointCache.put(endpointName, loadedConfig);
			return loadedConfig;
		} else {
			NonExsistentEndpointConfiguration endpointConfig = new NonExsistentEndpointConfiguration();
			endpointConfig.retrievalTimestamp = System.currentTimeMillis();
			this.endpointCache.put(endpointName, endpointConfig);
			return endpointConfig;
		}
	}

	private void mergeEndpointConfigs(EndpointConfiguration endpointConfiguration, EndpointConfiguration endpointToMerge) {
		if (endpointToMerge instanceof NonExsistentEndpointConfiguration) {
			return;
		}
		if (endpointConfiguration.eventEnhancer == null) {
			endpointConfiguration.eventEnhancer = endpointToMerge.eventEnhancer;
			return;
		}
		if (endpointConfiguration.eventEnhancer instanceof DefaultTelemetryEventEnhancer &&
			endpointToMerge.eventEnhancer instanceof DefaultTelemetryEventEnhancer) {
			((DefaultTelemetryEventEnhancer)endpointConfiguration.eventEnhancer).mergeFieldParsers((DefaultTelemetryEventEnhancer) endpointToMerge.eventEnhancer);
		}
	}

	@Override
	public void close() {
		this.etmConfiguration.removeConfigurationChangeListener(this);
		this.bulkProcessor.close();
	}
	
	private BulkProcessor createBulkProcessor() {
		return BulkProcessor.builder(this.elasticClient, this.bulkProcessorListener)
				.setBulkActions(this.etmConfiguration.getPersistingBulkCount() <= 0 ? -1 : this.etmConfiguration.getPersistingBulkCount())
				.setBulkSize(new ByteSizeValue(this.etmConfiguration.getPersistingBulkSize() <= 0 ? -1 : this.etmConfiguration.getPersistingBulkSize(), ByteSizeUnit.BYTES))
				.setFlushInterval(this.etmConfiguration.getPersistingBulkTime() <= 0 ? null : TimeValue.timeValueMillis(this.etmConfiguration.getPersistingBulkTime()))
				.build();
	}

	@Override
	public void configurationChanged(ConfigurationChangedEvent event) {
		if (event.isAnyChanged(EtmConfiguration.CONFIG_KEY_PERSISTING_BULK_COUNT, 
				EtmConfiguration.CONFIG_KEY_PERSISTING_BULK_SIZE, 
				EtmConfiguration.CONFIG_KEY_PERSISTING_BULK_TIME)) {
			BulkProcessor oldBulkProcessor = this.bulkProcessor;
			this.bulkProcessor = createBulkProcessor();
			this.persisters.values().forEach(c -> ((AbstractElasticTelemetryEventPersister)c).setBulkProcessor(this.bulkProcessor));
			oldBulkProcessor.close();
		}
	}
	
	/**
	 * Class to store in de endpoint cache to make sure the 
	 * 
	 * @author Mark Holster
	 */
	private class NonExsistentEndpointConfiguration extends EndpointConfiguration {}
	
	
}