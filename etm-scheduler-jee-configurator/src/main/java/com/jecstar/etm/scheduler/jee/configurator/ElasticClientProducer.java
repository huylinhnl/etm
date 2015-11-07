package com.jecstar.etm.scheduler.jee.configurator;

import java.net.InetAddress;

import javax.annotation.ManagedBean;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.jee.configurator.core.SchedulerConfiguration;

@ManagedBean
@Singleton
public class ElasticClientProducer {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(ElasticClientProducer.class);

	private Client elasticClient;

	@Produces
	@SchedulerConfiguration
	public Client getElasticClient() {
		synchronized (this) {
			if (this.elasticClient == null) {
				try {
					String clusterName = System.getProperty("etm.cluster.name");
					if (clusterName == null) {
						clusterName = "Enterprise Telemetry Monitor";
					}
					String clusterAddresses = System.getProperty("etm.cluster.addresses");
					if (clusterAddresses == null) {
						clusterAddresses = InetAddress.getLocalHost().getHostAddress() + ":9300";
					}
					
					Settings settings = ImmutableSettings.settingsBuilder().put("client.transport.sniff", true)
							.put("cluster.name", clusterName).build();
					String[] addresses = clusterAddresses.split(",");
					TransportClient transportClient = new TransportClient(settings);
					for (String address : addresses) {
						String[] split = address.split(":");
						 transportClient.addTransportAddress(new InetSocketTransportAddress(split[0], Integer.valueOf(split[1])));
					}
					this.elasticClient = transportClient;
                } catch (Exception e) {
                	this.elasticClient = null;
                	if (log.isErrorLevelEnabled()) {
                		log.logErrorMessage("Error creating elastic client.", e);
                	}
                }
			}
		}
		return this.elasticClient;
	}
	
	@PreDestroy
	public void preDestroy() {
		if (this.elasticClient != null) {
			this.elasticClient.close();
		}
	}
}
