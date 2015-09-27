package com.jecstar.etm.processor.elastic;

import java.util.Map;

import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.converter.EtmConfigurationConverter;
import com.jecstar.etm.core.domain.converter.EtmConfigurationConverterTags;
import com.jecstar.etm.core.domain.converter.json.EtmConfigurationConverterJsonImpl;

public class ElasticBackedEtmConfiguration extends EtmConfiguration {

	private final String indexName = "etm_configuration";
	private final String defaultId = "default_configuration";
	private final Client elasticClient;
	private final EtmConfigurationConverterTags tags;
	private final EtmConfigurationConverter<String> etmConfigurationConverter = new EtmConfigurationConverterJsonImpl();
	
	private final long updateCheckInterval = 60 * 1000;
	private long lastCheckedForUpdates;
	private long defaultVersion = -1;
	private long nodeVersion = -1;
	
	
	public ElasticBackedEtmConfiguration(String nodeName, String component, final Client elasticClient, final EtmConfigurationConverterTags etmConfigurationConverterTags) {
		super(nodeName, component);
		this.elasticClient = elasticClient;
		this.tags = etmConfigurationConverterTags;
		reloadConfiguration();
	}

	@Override
	public int getEnhancingHandlerCount() {
		if (reloadConfiguration()) {
			broadcastUpdate();
		}
		return super.getEnhancingHandlerCount();
	}
	
	@Override
	public int getPersistingHandlerCount() {
		if (reloadConfiguration()) {
			broadcastUpdate();
		}
		return super.getPersistingHandlerCount();
	}
	
	@Override
	public int getEventBufferSize() {
		if (reloadConfiguration()) {
			broadcastUpdate();
		}
		return super.getEventBufferSize();
	}

	@Override
	public int getPersistingBulkSize() {
		if (reloadConfiguration()) {
			broadcastUpdate();
		}
		return super.getPersistingBulkSize();
	}
	
	@Override
	public int getShardsPerIndex() {
		if (reloadConfiguration()) {
			broadcastUpdate();
		}
		return super.getShardsPerIndex();
	}
	
	
	@Override
	public int getReplicasPerIndex() {
		return super.getReplicasPerIndex();
	}
	
	private boolean reloadConfiguration() {
		boolean changed = false;
		if (System.currentTimeMillis() - this.lastCheckedForUpdates <= this.updateCheckInterval) {
			return changed;
		}
		GetResponse defaultResponse = this.elasticClient.prepareGet(this.indexName, getComponent(), this.defaultId).get();
		if (!defaultResponse.isExists()) {
			createDefault();
			defaultResponse = this.elasticClient.prepareGet(this.indexName, getComponent(), this.defaultId).get();
		}
		GetResponse nodeResponse = this.elasticClient.prepareGet(this.indexName, getComponent(), getNodeName()).get();

		Map<String, Object> defaultMap = defaultResponse.getSourceAsMap();
		Map<String, Object> nodeMap = null;

		if (defaultResponse.getVersion() != this.defaultVersion ||
				(nodeResponse.isExists() && nodeResponse.getVersion() != this.nodeVersion)) {
			if (nodeResponse.isExists()) {
				nodeMap = nodeResponse.getSourceAsMap();
				this.nodeVersion = nodeResponse.getVersion();
			}
			this.defaultVersion = defaultResponse.getVersion();
			EtmConfiguration etmConfiguration = new EtmConfiguration("temp-for-reload-merge", getComponent());
			etmConfiguration.setEnhancingHandlerCount(getIntValue(tags.getEnhancingHandlerCountTag(), defaultMap, nodeMap));
			etmConfiguration.setPersistingHandlerCount(getIntValue(tags.getPersistingHandlerCountTag(), defaultMap, nodeMap));
			etmConfiguration.setEventBufferSize(getIntValue(tags.getEventBufferSizeTag(), defaultMap, nodeMap));
			etmConfiguration.setPersistingBulkSize(getIntValue(tags.getPersistingBulkSizeTag(), defaultMap, nodeMap));
			etmConfiguration.setShardsPerIndex(getIntValue(tags.getShardsPerIndexTag(), defaultMap, nodeMap));
			etmConfiguration.setReplicasPerIndex(getIntValue(tags.getReplicasPerIndexTag(), defaultMap, nodeMap));
			changed = this.merge(etmConfiguration);
		}
		this.lastCheckedForUpdates = System.currentTimeMillis();
		return changed;
	}
	
	private Integer getIntValue(String tag, Map<String, Object> defaultMap, Map<String, Object> nodeMap) {
		if (nodeMap != null && nodeMap.containsKey(tag)) {
			return ((Number) nodeMap.get(tag)).intValue();
		} else {
			return ((Number) defaultMap.get(tag)).intValue();
		}
	}

	private void createDefault() {
		new PutIndexTemplateRequestBuilder(this.elasticClient.admin().indices(), this.indexName)
			.setCreate(false)
			.setTemplate(this.indexName)
			.setSettings(ImmutableSettings.settingsBuilder()
					.put("number_of_shards", 1)
					.put("number_of_replicas", 5)
					.build())
			.addMapping("_default_", createMapping("_default_"))
			.get();
		this.elasticClient.prepareIndex(this.indexName, getComponent(), this.defaultId)
			.setConsistencyLevel(WriteConsistencyLevel.ONE)
			.setSource(this.etmConfigurationConverter.convert(null, this, this.tags)).get();
	}

	private String createMapping(String string) {
		return "{" + 
				"   \"properties\": {" + 
				"	    \"" + this.tags.getEnhancingHandlerCountTag() +"\": {" + 
				"   	    \"type\": \"integer\"," +
				"           \"index\": \"not_analyzed\"" + 				
				"       }," + 
				"	    \"" + this.tags.getPersistingHandlerCountTag() + "\": {" + 
				"   	    \"type\": \"integer\"," +
				"           \"index\": \"not_analyzed\"" + 				
				"       }," + 
				"	    \"" + this.tags.getEventBufferSizeTag() + "\": {" + 
				"   	    \"type\": \"integer\"," +
				"           \"index\": \"not_analyzed\"" + 				
				"       }," + 
				"	    \"" + this.tags.getPersistingBulkSizeTag() + "\": {" + 
				"   	    \"type\": \"integer\"," +
				"           \"index\": \"not_analyzed\"" + 				
				"       }," + 
				"	    \"" + this.tags.getShardsPerIndexTag() + "\": {" + 
				"   	    \"type\": \"integer\"," +
				"           \"index\": \"not_analyzed\"" + 				
				"       }," + 
				"	    \"" + this.tags.getReplicasPerIndexTag() + "\": {" + 
				"   	    \"type\": \"integer\"," +
				"           \"index\": \"not_analyzed\"" + 				
				"       }" + 
				"    }" + 
				"}";	
		}

}
