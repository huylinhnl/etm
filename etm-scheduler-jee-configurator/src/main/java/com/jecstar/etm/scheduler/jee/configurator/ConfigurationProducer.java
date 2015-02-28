package com.jecstar.etm.scheduler.jee.configurator;

import javax.annotation.ManagedBean;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.jee.configurator.core.SchedulerConfiguration;

@ManagedBean
@Singleton
public class ConfigurationProducer {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(ConfigurationProducer.class);

	private EtmConfiguration configuration;

	@Produces
	@SchedulerConfiguration
	public EtmConfiguration getEtmConfiguration() {
		synchronized (this) {
			if (this.configuration == null) {
				try {
					String nodename = System.getProperty("etm.nodename");
					String connections = System.getProperty("etm.zookeeper.clients", "127.0.0.1:2181/etm");
					String namespace = System.getProperty("etm.zookeeper.namespace", "dev");
	                this.configuration = new EtmConfiguration(nodename, connections, namespace, "scheduler");
                } catch (Exception e) {
                	this.configuration = null;
                	if (log.isErrorLevelEnabled()) {
                		log.logErrorMessage("Error loading etm configuration.", e);
                	}
                }
			}
		}
		return this.configuration;
	}
	
	@PreDestroy
	public void preDestroy() {
		if (this.configuration != null) {
			this.configuration.close();
		}
	}
}