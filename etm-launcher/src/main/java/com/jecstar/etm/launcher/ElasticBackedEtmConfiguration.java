package com.jecstar.etm.launcher;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;

import com.jecstar.etm.server.core.configuration.ElasticSearchLayout;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.configuration.License;
import com.jecstar.etm.server.core.configuration.WriteConsistency;
import com.jecstar.etm.server.core.domain.converter.EtmConfigurationConverter;
import com.jecstar.etm.server.core.domain.converter.json.EtmConfigurationConverterJsonImpl;

public class ElasticBackedEtmConfiguration extends EtmConfiguration {

	private final Client elasticClient;
	private final EtmConfigurationConverter<String> etmConfigurationConverter = new EtmConfigurationConverterJsonImpl();
	
	private final long updateCheckInterval = 60 * 1000;
	private long lastCheckedForUpdates;
	
	public ElasticBackedEtmConfiguration(String nodeName, final Client elasticClient) {
		super(nodeName);
		this.elasticClient = elasticClient;
		reloadConfigurationWhenNecessary();
	}
	
	@Override
	public License getLicense() {
		reloadConfigurationWhenNecessary();
		return super.getLicense();
	}

	@Override
	public int getEnhancingHandlerCount() {
		reloadConfigurationWhenNecessary();
		return super.getEnhancingHandlerCount();
	}
	
	@Override
	public int getPersistingHandlerCount() {
		reloadConfigurationWhenNecessary();
		return super.getPersistingHandlerCount();
	}
	
	@Override
	public int getEventBufferSize() {
		reloadConfigurationWhenNecessary();
		return super.getEventBufferSize();
	}

	@Override
	public int getPersistingBulkSize() {
		reloadConfigurationWhenNecessary();
		return super.getPersistingBulkSize();
	}
	
	@Override
	public int getPersistingBulkCount() {
		reloadConfigurationWhenNecessary();
		return super.getPersistingBulkCount();
	}
	
	@Override
	public int getPersistingBulkTime() {
		reloadConfigurationWhenNecessary();
		return super.getPersistingBulkTime();
	}
	
	@Override
	public int getShardsPerIndex() {
		reloadConfigurationWhenNecessary();
		return super.getShardsPerIndex();
	}
	
	@Override
	public int getReplicasPerIndex() {
		reloadConfigurationWhenNecessary();
		return super.getReplicasPerIndex();
	}

	@Override
	public int getMaxEventIndexCount() {
		reloadConfigurationWhenNecessary();
		return super.getMaxEventIndexCount();
	}
	
	@Override
	public int getMaxMetricsIndexCount() {
		reloadConfigurationWhenNecessary();
		return super.getMaxMetricsIndexCount();
	}
	
	@Override
	public WriteConsistency getWriteConsistency() {
		reloadConfigurationWhenNecessary();
		return super.getWriteConsistency();
	}
	
	@Override
	public long getQueryTimeout() {
		reloadConfigurationWhenNecessary();
		return super.getQueryTimeout();
	}
	
	@Override
	public int getRetryOnConflictCount() {
		reloadConfigurationWhenNecessary();
		return super.getRetryOnConflictCount();
	}
	
	@Override
	public int getMaxSearchResultDownloadRows() {
		reloadConfigurationWhenNecessary();
		return super.getMaxSearchResultDownloadRows();
	}
	
	@Override
	public boolean isLicenseExpired() {
		reloadConfigurationWhenNecessary();
		return super.isLicenseExpired();
	}
	
	@Override
	public Boolean isLicenseAlmostExpired() {
		reloadConfigurationWhenNecessary();
		return super.isLicenseAlmostExpired();
	}
	
	private boolean reloadConfigurationWhenNecessary() {
		boolean changed = false;
		if (System.currentTimeMillis() - this.lastCheckedForUpdates <= this.updateCheckInterval) {
			return changed;
		}
		GetResponse defaultResponse = this.elasticClient.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_NODE, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_NODE_DEFAULT).get();
		GetResponse nodeResponse = this.elasticClient.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_NODE, getNodeName()).get();
		GetResponse licenseResponse = this.elasticClient.prepareGet(ElasticSearchLayout.CONFIGURATION_INDEX_NAME, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_LICENSE, ElasticSearchLayout.CONFIGURATION_INDEX_TYPE_LICENSE_ID).get();

		String defaultContent = defaultResponse.getSourceAsString();
		String nodeContent = null;

		if (nodeResponse.isExists()) {
			nodeContent = nodeResponse.getSourceAsString();
		}
		EtmConfiguration etmConfiguration = this.etmConfigurationConverter.read(nodeContent, defaultContent, "temp-for-reload-merge");
		if (licenseResponse.isExists() && !licenseResponse.isSourceEmpty()) {
			Object license = licenseResponse.getSourceAsMap().get(this.etmConfigurationConverter.getTags().getLicenseTag());
			if (license != null) {
				etmConfiguration.setLicenseKey(license.toString());
			}
		}
		changed = this.mergeAndNotify(etmConfiguration);
		this.lastCheckedForUpdates = System.currentTimeMillis();
		return changed;
	}
}
