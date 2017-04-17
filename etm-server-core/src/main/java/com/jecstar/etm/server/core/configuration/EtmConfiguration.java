package com.jecstar.etm.server.core.configuration;

import java.time.Instant;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.ldap.Directory;
import com.jecstar.etm.server.core.util.ObjectUtils;

public class EtmConfiguration {
	//License configuration
	public static final String CONFIG_KEY_LICENSE 							= "license";
	// Cluster configuration
	public static final String CONFIG_KEY_SHARDS_PER_INDEX 					= "shardsPerIndex";
	public static final String CONFIG_KEY_REPLICAS_PER_INDEX 				= "replicasPerIndex";
	public static final String CONFIG_KEY_MAX_EVENT_INDEX_COUNT 			= "maxEventIndexCount";
	public static final String CONFIG_KEY_MAX_METRICS_INDEX_COUNT 			= "maxMetricsIndexCount";
	public static final String CONFIG_KEY_MAX_AUDIT_LOG_INDEX_COUNT 		= "maxAuditLogIndexCount";	
	public static final String CONFIG_KEY_WAIT_FOR_ACTIVE_SHARDS 			= "waitForActiveShards";
	public static final String CONFIG_KEY_QUERY_TIMEOUT 					= "queryTimeout";
	public static final String CONFIG_KEY_RETRY_ON_CONFLICT_COUNT 			= "retryOnConflictCount";
	public static final String CONFIG_KEY_MAX_SEARCH_RESULT_DOWNLOAD_ROWS 	= "maxSearchResultDownloadRows";
	public static final String CONFIG_KEY_MAX_SEARCH_TEMPLATE_COUNT 		= "maxSearchTemplateCount";
	public static final String CONFIG_KEY_MAX_SEARCH_HISTORY_COUNT 			= "maxSearchHistoryCount";
	public static final String CONFIG_KEY_MAX_GRAPH_COUNT 					= "maxGraphCount";
	public static final String CONFIG_KEY_MAX_DASHBOARD_COUNT 				= "maxDashboardCount";
	
	// Node configurations
	public static final String CONFIG_KEY_ENHANCING_HANDLER_COUNT 			= "enhancingHandlerCount";
	public static final String CONFIG_KEY_PERSISTING_HANDLER_COUNT 			= "persistingHandlerCount";
	public static final String CONFIG_KEY_EVENT_BUFFER_SIZE 				= "eventBufferSize";
	public static final String CONFIG_KEY_PERSISTING_BULK_COUNT 			= "persistingBulkCount";
	public static final String CONFIG_KEY_PERSISTING_BULK_SIZE 				= "persistingBulkSize";
	public static final String CONFIG_KEY_PERSISTING_BULK_TIME 				= "persistingBulkTime";
	public static final String CONFIG_KEY_WAIT_STRATEGY 					= "waitStrategy";
	

	// Disruptor configuration properties.
	private int enhancingHandlerCount = 5;
	private int persistingHandlerCount = 5;
	private int eventBufferSize = 4096;
	private WaitStrategy waitStrategy = WaitStrategy.BLOCKING;
	
	// Persisting configuration properties;
	private int persistingBulkSize = 1024 * 1024 * 5;
	private int persistingBulkCount = 1000;
	private int persistingBulkTime = 5000;
	
	private int shardsPerIndex = 5;
	private int replicasPerIndex = 0;
	private int waitForActiveShards = 1;
	private int retryOnConflictCount = 3;
	
	// Data configuration properties;
	private int maxEventIndexCount = 7; 
	private int maxMetricsIndexCount = 7;
	private int maxAuditLogIndexCount = 7; 

	// Query options
	private long queryTimeout = 60 * 1000;
	private int maxSearchResultDownloadRows = 500;
	private int maxSearchTemplateCount = 10;
	private int maxSearchHistoryCount = 10;
	
	// Visualization options
	private int maxGraphCount = 100;
	private int maxDashboardCount = 10;
	
	// Other stuff.		
	private final String nodeName;

	private License license;
	
	private Directory directory;

	private List<ConfigurationChangeListener> changeListeners = new ArrayList<ConfigurationChangeListener>();

	public EtmConfiguration(String nodeName) {
		this.nodeName = nodeName;
	}

	// Etm license configuration

	public License getLicense() {
		return this.license;
	}

	public void setLicenseKey(String licenseKey) {
		if (licenseKey == null || licenseKey.trim().length() == 0) {
			throw new EtmException(EtmException.INVALID_LICENSE_KEY_EXCEPTION);
		}		
		this.license = new License(licenseKey);
	}
	
	public Directory getDirectory() {
		return this.directory;
	}
	
	public void setDirectory(Directory directory) {
		if (this.directory != null) {
			this.directory.close();
		}
		this.directory = directory;
	}
	
	/**
	 * Method to determine if a license key is valid. This method does not check
	 * if the license is expired!
	 * 
	 * @param licenseKey
	 *            The key to check.
	 * @return <code>true</code> if the license is syntactically correct,
	 *         <code>false</code> otherwise.
	 */
	public boolean isValidLicenseKey(String licenseKey) {
		if (licenseKey == null || licenseKey.trim().length() == 0) {
			return false;
		}		
		try {
			new License(licenseKey);
			return true;
		} catch (EtmException e) {
		}
		return false;
	}
	
	// Etm processor configuration

	public int getEnhancingHandlerCount() {
		return this.enhancingHandlerCount;
	}
	
	public EtmConfiguration setEnhancingHandlerCount(Integer enhancingHandlerCount) {
		if (enhancingHandlerCount != null && enhancingHandlerCount >= 0) {
			this.enhancingHandlerCount = enhancingHandlerCount;
		}
		return this;
	}

	public int getPersistingHandlerCount() {
		return this.persistingHandlerCount;
	}
	
	public EtmConfiguration setPersistingHandlerCount(Integer persistingHandlerCount) {
		if (persistingHandlerCount != null && persistingHandlerCount >= 0) {
			this.persistingHandlerCount = persistingHandlerCount;
		}
		return this;
	}

	public int getEventBufferSize() {
		return this.eventBufferSize;
	}
	
	public EtmConfiguration setEventBufferSize(Integer eventBufferSize) {
		if (eventBufferSize != null && eventBufferSize > 0) {
			this.eventBufferSize = eventBufferSize;
		}
		return this;
	}
	
	public WaitStrategy getWaitStrategy() {
		return this.waitStrategy;
	}
	
	public EtmConfiguration setWaitStrategy(WaitStrategy waitStrategy) {
		if (waitStrategy != null) {
			this.waitStrategy = waitStrategy;
		}
		return this;
	}

	// Etm persisting configuration.
	public int getPersistingBulkSize() {
		return this.persistingBulkSize;
	}
	
	public EtmConfiguration setPersistingBulkSize(Integer persistingBulkSize) {
		if (persistingBulkSize != null && persistingBulkSize >= 0) {
			this.persistingBulkSize = persistingBulkSize;
		}
		return this;
	}
	
	public int getPersistingBulkCount() {
		return this.persistingBulkCount;
	}
	
	public EtmConfiguration setPersistingBulkCount(Integer persistingBulkCount) {
		if (persistingBulkCount != null && persistingBulkCount >= 0) {
			this.persistingBulkCount = persistingBulkCount;
		}
		return this;
	}
	
	public int getPersistingBulkTime() {
		return this.persistingBulkTime;
	}
	
	public EtmConfiguration setPersistingBulkTime(Integer persistingBulkTime) {
		if (persistingBulkTime != null && persistingBulkTime >= 0) {
			this.persistingBulkTime = persistingBulkTime;
		}
		return this;
	}
	
	public int getShardsPerIndex() {
		return this.shardsPerIndex;
	}
	
	public EtmConfiguration setShardsPerIndex(Integer shardsPerIndex) {
		if (shardsPerIndex != null && shardsPerIndex > 0) {
			this.shardsPerIndex = shardsPerIndex;
		} 
		return this;
	}

	public int getReplicasPerIndex() {
		return this.replicasPerIndex;
	}
	
	public EtmConfiguration setReplicasPerIndex(Integer replicasPerIndex) {
		if (replicasPerIndex != null && replicasPerIndex >= 0) {
			this.replicasPerIndex = replicasPerIndex;
		}
		return this;
	}
	
	public int getMaxEventIndexCount() {
		return this.maxEventIndexCount;
	}
	
	public EtmConfiguration setMaxEventIndexCount(Integer maxEventIndexCount) {
		if (maxEventIndexCount != null && maxEventIndexCount > 0) {
			this.maxEventIndexCount = maxEventIndexCount;
		}
		return this;
	}

	public int getMaxMetricsIndexCount() {
		return this.maxMetricsIndexCount;
	}
	
	public EtmConfiguration setMaxMetricsIndexCount(Integer maxMetricsIndexCount) {
		if (maxMetricsIndexCount != null && maxMetricsIndexCount > 0) {
			this.maxMetricsIndexCount = maxMetricsIndexCount;
		}
		return this;
	}
	
	public int getMaxAuditLogIndexCount() {
		return this.maxMetricsIndexCount;
	}
	
	public EtmConfiguration setMaxAuditLogIndexCount(Integer maxAuditLogIndexCount) {
		if (maxAuditLogIndexCount != null && maxAuditLogIndexCount >= 7) {
			this.maxAuditLogIndexCount = maxAuditLogIndexCount;
		}
		return this;
	}

	
	public int getWaitForActiveShards() {
		return this.waitForActiveShards;
	}
	
	public EtmConfiguration setWaitForActiveShards(Integer waitForActiveShards) {
		if (waitForActiveShards != null && waitForActiveShards >= -1) {
			this.waitForActiveShards = waitForActiveShards;
		}
		return this;
	}
	
	public int getRetryOnConflictCount() {
		return this.retryOnConflictCount;
	}
	
	public EtmConfiguration setRetryOnConflictCount(Integer retryOnConflictCount) {
		if (retryOnConflictCount != null && retryOnConflictCount >= 0) {
			this.retryOnConflictCount = retryOnConflictCount;
		}
		return this;
	}
	
	public long getQueryTimeout() {
		return this.queryTimeout;
	}
	
	public EtmConfiguration setQueryTimeout(Long queryTimeout) {
		if (queryTimeout != null && queryTimeout > 0) {
			this.queryTimeout = queryTimeout;
		}
		return this;
	}

	public int getMaxSearchResultDownloadRows() {
		return this.maxSearchResultDownloadRows;
	}
	
	public EtmConfiguration setMaxSearchResultDownloadRows(Integer maxSearchResultDownloadRows) {
		if (maxSearchResultDownloadRows != null && maxSearchResultDownloadRows > 0) {
			this.maxSearchResultDownloadRows = maxSearchResultDownloadRows;
		}
		return this;
	}
	
	public int getMaxSearchHistoryCount() {
		return this.maxSearchHistoryCount;
	}
	
	public EtmConfiguration setMaxSearchHistoryCount(Integer maxSearchHistoryCount) {
		if (maxSearchHistoryCount != null && maxSearchHistoryCount >=  0) {
			this.maxSearchHistoryCount = maxSearchHistoryCount;
		}
		return this;
	}
	
	public int getMaxSearchTemplateCount() {
		return this.maxSearchTemplateCount;
	}
	
	public EtmConfiguration setMaxSearchTemplateCount(Integer maxSearchTemplateCount) {
		if (maxSearchTemplateCount != null && maxSearchTemplateCount >= 0) {
			this.maxSearchTemplateCount = maxSearchTemplateCount;
		}
		return this;
	}
	
	public int getMaxGraphCount() {
		return this.maxGraphCount;
	}
	
	public EtmConfiguration setMaxGraphCount(Integer maxGraphCount) {
		if (maxGraphCount != null && maxGraphCount >= 0) {
			this.maxGraphCount = maxGraphCount;
		}
		return this;
	}
	
	public int getMaxDashboardCount() {
		return maxDashboardCount;
	}
	
	public EtmConfiguration setMaxDashboardCount(Integer maxDashboardCount) {
		if (maxDashboardCount != null && maxDashboardCount >= 0) {
			this.maxDashboardCount = maxDashboardCount;
		}
		return this;
	}

	public String getNodeName() {
		return this.nodeName;
	}
	
	public void addConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener) {
		if (!this.changeListeners.contains(configurationChangeListener)) {
			this.changeListeners.add(configurationChangeListener);
		}
	}
	
	public void removeConfigurationChangeListener(ConfigurationChangeListener configurationChangeListener) {
		this.changeListeners.remove(configurationChangeListener);
	}
	
	public boolean isLicenseExpired() {
		if (this.license == null) {
			return true;
		}
		return !isLicenseValidAt(Instant.now());
	}

	public Boolean isLicenseAlmostExpired() {
		if (this.license == null) {
			return false;
		}		
		return !isLicenseExpired() && !isLicenseValidAt(Instant.now().plus(Period.ofDays(14)));
	}
	
	public Boolean isLicenseCountExceeded() {
		if (this.license == null) {
			return true;
		}
		return false;
	}
	
	public Boolean isLicenseSizeExceeded() {
		if (this.license == null) {
			return true;
		}
		return false;
	}
	
	private boolean isLicenseValidAt(Instant moment) {
		if (this.license == null) {
			return false;
		}
		return !moment.isAfter(this.license.getExpiryDate());
	}

	/**
	 * Merge the configuration items from the given
	 * <code>EtmConfiguration</code> into this instance.
	 * <code>ConfigurationChangeListener</code>s are notified in a single event.
	 * 
	 * @param etmConfiguration
	 *            The <code>EtmConfiguration</code> to merge into this instance.
	 * @return <code>true</code> when the configuration is changed,
	 *         <code>false</code> otherwise.
	 */
	public synchronized boolean mergeAndNotify(EtmConfiguration etmConfiguration) {
		if (etmConfiguration == null) {
			return false;
		}
		
		List<String> changed = new ArrayList<String>();
		if (!ObjectUtils.equalsNullProof(this.license, etmConfiguration.getLicense())) {
			this.license = etmConfiguration.getLicense();
			changed.add(CONFIG_KEY_LICENSE);
		}
		if (this.enhancingHandlerCount != etmConfiguration.getEnhancingHandlerCount()) {
			setEnhancingHandlerCount(etmConfiguration.getEnhancingHandlerCount());
			changed.add(CONFIG_KEY_ENHANCING_HANDLER_COUNT);
		}
		if (this.persistingHandlerCount != etmConfiguration.getPersistingHandlerCount()) {
			setPersistingHandlerCount(etmConfiguration.getPersistingHandlerCount());
			changed.add(CONFIG_KEY_PERSISTING_HANDLER_COUNT);
		}
		if (this.eventBufferSize != etmConfiguration.getEventBufferSize()) {
			setEventBufferSize(etmConfiguration.getEventBufferSize());
			changed.add(CONFIG_KEY_EVENT_BUFFER_SIZE);
		}
		if (!this.waitStrategy.equals(etmConfiguration.waitStrategy)) {
			setWaitStrategy(etmConfiguration.waitStrategy);
			changed.add(CONFIG_KEY_WAIT_STRATEGY);
		}
		if (this.persistingBulkSize != etmConfiguration.getPersistingBulkSize()) {
			setPersistingBulkSize(etmConfiguration.getPersistingBulkSize());
			changed.add(CONFIG_KEY_PERSISTING_BULK_SIZE);
		}
		if (this.persistingBulkCount != etmConfiguration.getPersistingBulkCount()) {
			setPersistingBulkCount(etmConfiguration.getPersistingBulkCount());
			changed.add(CONFIG_KEY_PERSISTING_BULK_COUNT);
		}		
		if (this.persistingBulkTime != etmConfiguration.getPersistingBulkTime()) {
			setPersistingBulkTime(etmConfiguration.getPersistingBulkTime());
			changed.add(CONFIG_KEY_PERSISTING_BULK_TIME);
		}		
		if (this.shardsPerIndex != etmConfiguration.getShardsPerIndex()) {
			setShardsPerIndex(etmConfiguration.getShardsPerIndex());
			changed.add(CONFIG_KEY_SHARDS_PER_INDEX);
		}
		if (this.replicasPerIndex != etmConfiguration.getReplicasPerIndex()) {
			setReplicasPerIndex(etmConfiguration.replicasPerIndex);
			changed.add(CONFIG_KEY_REPLICAS_PER_INDEX);
		}
		if (this.maxEventIndexCount != etmConfiguration.getMaxEventIndexCount()) {
			 setMaxEventIndexCount(etmConfiguration.getMaxEventIndexCount());
			 changed.add(CONFIG_KEY_MAX_EVENT_INDEX_COUNT);
		}
		if (this.maxMetricsIndexCount != etmConfiguration.getMaxMetricsIndexCount()) {
			 setMaxMetricsIndexCount(etmConfiguration.getMaxMetricsIndexCount());
			 changed.add(CONFIG_KEY_MAX_METRICS_INDEX_COUNT);
		}
		if (this.maxAuditLogIndexCount != etmConfiguration.getMaxAuditLogIndexCount()) {
			 setMaxAuditLogIndexCount(etmConfiguration.getMaxAuditLogIndexCount());
			 changed.add(CONFIG_KEY_MAX_AUDIT_LOG_INDEX_COUNT);
		}
		if (this.waitForActiveShards != etmConfiguration.getWaitForActiveShards()) {
			setWaitForActiveShards(etmConfiguration.getWaitForActiveShards());
			changed.add(CONFIG_KEY_WAIT_FOR_ACTIVE_SHARDS);
		}
		if (this.queryTimeout != etmConfiguration.getQueryTimeout()) {
			setQueryTimeout(etmConfiguration.getQueryTimeout());
			changed.add(CONFIG_KEY_QUERY_TIMEOUT);
		}
		if (this.retryOnConflictCount != etmConfiguration.getRetryOnConflictCount()) {
			setRetryOnConflictCount(etmConfiguration.getRetryOnConflictCount());
			changed.add(CONFIG_KEY_RETRY_ON_CONFLICT_COUNT);
		}
		if (this.maxSearchResultDownloadRows != etmConfiguration.getMaxSearchResultDownloadRows()) {
			setMaxSearchResultDownloadRows(etmConfiguration.getMaxSearchResultDownloadRows());
			changed.add(CONFIG_KEY_MAX_SEARCH_RESULT_DOWNLOAD_ROWS);
		}
		if (this.maxSearchTemplateCount != etmConfiguration.getMaxSearchTemplateCount()) {
			setMaxSearchTemplateCount(etmConfiguration.getMaxSearchTemplateCount());
			changed.add(CONFIG_KEY_MAX_SEARCH_TEMPLATE_COUNT);
		}
		if (this.maxSearchHistoryCount != etmConfiguration.getMaxSearchHistoryCount()) {
			setMaxSearchHistoryCount(etmConfiguration.getMaxSearchHistoryCount());
			changed.add(CONFIG_KEY_MAX_SEARCH_HISTORY_COUNT);
		}
		if (this.maxGraphCount != etmConfiguration.getMaxGraphCount()) {
			setMaxGraphCount(etmConfiguration.getMaxGraphCount());
			changed.add(CONFIG_KEY_MAX_GRAPH_COUNT);
		}
		if (this.maxDashboardCount != etmConfiguration.getMaxDashboardCount()) {
			setMaxDashboardCount(etmConfiguration.getMaxDashboardCount());
			changed.add(CONFIG_KEY_MAX_DASHBOARD_COUNT);
		}
		if (changed.size() > 0) {
			ConfigurationChangedEvent event = new ConfigurationChangedEvent(changed);
			this.changeListeners.forEach(c -> c.configurationChanged(event));
		}
		return changed.size() > 0;
	}
}
