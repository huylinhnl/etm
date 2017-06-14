package com.jecstar.etm.launcher;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.jecstar.etm.launcher.background.HttpSessionCleaner;
import com.jecstar.etm.launcher.background.IndexCleaner;
import com.jecstar.etm.launcher.background.LicenseUpdater;
import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.launcher.http.ElasticsearchIdentityManager;
import com.jecstar.etm.launcher.http.HttpServer;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.elastic.PersistenceEnvironmentElasticImpl;
import com.jecstar.etm.processor.ibmmq.IbmMqProcessor;
import com.jecstar.etm.processor.ibmmq.configuration.IbmMq;
import com.jecstar.etm.processor.internal.persisting.BusinessEventLogger;
import com.jecstar.etm.processor.internal.persisting.InternalBulkProcessorWrapper;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

public class Launcher {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(Launcher.class);
	
	private ElasticsearchIndextemplateCreator indexTemplateCreator;
	private TelemetryCommandProcessor processor;
	private HttpServer httpServer;
	private Client elasticClient;
	private ScheduledReporter metricReporter;
	private IbmMqProcessor ibmMqProcessor;
	private ScheduledExecutorService backgroundScheduler;
	private InternalBulkProcessorWrapper bulkProcessorWrapper;
	
	public void launch(CommandLineParameters commandLineParameters, Configuration configuration, InternalBulkProcessorWrapper bulkProcessorWrapper) {
		this.bulkProcessorWrapper = bulkProcessorWrapper;
		addShutdownHooks(configuration);
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
		try {
			initializeElasticsearchClient(configuration);
			this.bulkProcessorWrapper.setClient(this.elasticClient);
			this.indexTemplateCreator = new ElasticsearchIndextemplateCreator(this.elasticClient);
			this.indexTemplateCreator.createTemplates();
			EtmConfiguration etmConfiguration = new ElasticBackedEtmConfiguration(configuration.instanceName, this.elasticClient);
			this.bulkProcessorWrapper.setConfiguration(etmConfiguration);
			this.indexTemplateCreator.addConfigurationChangeNotificationListener(etmConfiguration);
			MetricRegistry metricRegistry = new MetricRegistry();
			initializeMetricReporter(metricRegistry, configuration);
			initializeProcessor(metricRegistry, configuration, etmConfiguration);
			initializeBackgroundProcesses(configuration, etmConfiguration, this.elasticClient);
			
			if (configuration.isHttpServerNecessary()) {
				System.setProperty("org.jboss.logging.provider", "slf4j");
				this.httpServer = new HttpServer(new ElasticsearchIdentityManager(this.elasticClient, etmConfiguration), configuration, etmConfiguration, this.processor, this.elasticClient);
				this.httpServer.start();
			}
			if (configuration.ibmMq.enabled) {
				initializeMqProcessor(metricRegistry, configuration);
			}
			if (!commandLineParameters.isQuiet()) {
				System.out.println("Enterprise Telemetry Monitor started.");
			}
			if (log.isInfoLevelEnabled()) {
				log.logInfoMessage("Enterprise Telemetry Monitor started.");
			}
			BusinessEventLogger.logEtmStartup();
		} catch (Exception e) {
			if (!commandLineParameters.isQuiet()) {
				e.printStackTrace();
			}
			if (log.isFatalLevelEnabled()) {
				log.logFatalMessage("Error launching Enterprise Telemetry Monitor", e);
			}
		}		
	}

	private void addShutdownHooks(Configuration configuration) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (log.isInfoLevelEnabled()) {
					log.logInfoMessage("Shutting down Enterprise Telemetry Monitor.");
				}
				if (Launcher.this.indexTemplateCreator != null) {
					try { Launcher.this.indexTemplateCreator.removeConfigurationChangeNotificationListener(); } catch (Throwable t) {}
				}
				if (Launcher.this.backgroundScheduler != null) {
					try { Launcher.this.backgroundScheduler.shutdownNow(); } catch (Throwable t) {}
				}
				if (Launcher.this.ibmMqProcessor != null) {
					try { Launcher.this.ibmMqProcessor.stop(); } catch (Throwable t) {}
				}
				if (Launcher.this.httpServer != null) {
					try { Launcher.this.httpServer.stop(); } catch (Throwable t) {}
				}
				if (Launcher.this.processor != null) {
					try { Launcher.this.processor.stopAll(); } catch (Throwable t) {}
				}
				if (Launcher.this.metricReporter != null) {
					try { Launcher.this.metricReporter.close(); } catch (Throwable t) {}
				}
				if (Launcher.this.bulkProcessorWrapper != null) {
					try { 
						BusinessEventLogger.logEtmShutdown();
						Launcher.this.bulkProcessorWrapper.close(); 
					} catch (Throwable t) {}
				}
				if (Launcher.this.elasticClient != null) {
					try { Launcher.this.elasticClient.close(); } catch (Throwable t) {}
				}
			}
		});
	}
	
	
	private void initializeBackgroundProcesses(final Configuration configuration, final EtmConfiguration etmConfiguration, final Client client) {
		int threadPoolSize = 2;
		if (configuration.http.guiEnabled || configuration.http.restProcessorEnabled) {
			threadPoolSize++;
		}
		this.backgroundScheduler = new ScheduledThreadPoolExecutor(threadPoolSize, new NamedThreadFactory("etm_background_scheduler"));
		this.backgroundScheduler.scheduleAtFixedRate(new IndexCleaner(etmConfiguration, client), 0, 15, TimeUnit.MINUTES);
		this.backgroundScheduler.scheduleAtFixedRate(new LicenseUpdater(etmConfiguration, client), 0, 6, TimeUnit.HOURS);
		if (configuration.http.guiEnabled) {
			this.backgroundScheduler.scheduleAtFixedRate(new HttpSessionCleaner(etmConfiguration, client), 0, 15, TimeUnit.MINUTES);
		}
		
	}

	
	private void initializeProcessor(MetricRegistry metricRegistry, Configuration configuration, EtmConfiguration etmConfiguration) {
		if (this.processor == null) {
			this.processor = new TelemetryCommandProcessor(metricRegistry);
			this.processor.start(new NamedThreadFactory("etm_processor"), new PersistenceEnvironmentElasticImpl(etmConfiguration, this.elasticClient), etmConfiguration);
		}
	}
	
	private void initializeElasticsearchClient(Configuration configuration) {
		if (this.elasticClient != null) {
			return;
		}
		TransportClient transportClient = new PreBuiltTransportClient(Settings.builder()
				.put("cluster.name", configuration.elasticsearch.clusterName)
				.put("client.transport.sniff", true).build());
		String[] hosts = configuration.elasticsearch.connectAddresses.split(",");
		for (String host : hosts) {
			// TODO dit gaat niet goed in Docker als de etm sevice opkomt voordat de es services er zijn. De hostname van es bestaat dan nog niet.
			int ix = host.lastIndexOf(":");
			if (ix != -1) {
				try {
					InetAddress inetAddress = InetAddress.getByName(host.substring(0, ix));
					int port = Integer.parseInt(host.substring(ix + 1));
					transportClient.addTransportAddress(new InetSocketTransportAddress(inetAddress, port));
				} catch (UnknownHostException e) {
					if (log.isWarningLevelEnabled()) {
						log.logWarningMessage("Unable to connect to '" + host + "'", e);
					}
				}
			}
		}
		if (configuration.elasticsearch.waitForConnectionOnStartup) {
			waitForActiveConnection(transportClient);
		}
		this.elasticClient = transportClient;
	}
	
	private void waitForActiveConnection(TransportClient transportClient) {
		while(transportClient.connectedNodes().isEmpty()) {
			// Wait for any elasticsearch node to become active.
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
		}
		ClusterHealthResponse clusterHealthResponse = transportClient.admin().cluster().prepareHealth().get();
		while (clusterHealthResponse.getInitializingShards() != 0 && clusterHealthResponse.getNumberOfPendingTasks() != 0) {
			// Wait for all shards to be initialized and no more tasks pending.
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if (Thread.currentThread().isInterrupted()) {
				return;
			}			
			clusterHealthResponse = transportClient.admin().cluster().prepareHealth().get();
		}
	
		
	}

	private void initializeMqProcessor(MetricRegistry metricRegistry, Configuration configuration) {
		try {
			Class<?> clazz = Class.forName("com.jecstar.etm.processor.ibmmq.IbmMqProcessorImpl");
			this.ibmMqProcessor = (IbmMqProcessor) clazz
					.getConstructor(
							TelemetryCommandProcessor.class, 
							MetricRegistry.class,
							IbmMq.class, 
							String.class,
							String.class
						).newInstance(
							this.processor, 
							metricRegistry,
							configuration.ibmMq, 
							configuration.clusterName, 
							configuration.instanceName);
			this.ibmMqProcessor.start();
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			if (log.isWarningLevelEnabled()) {
				log.logWarningMessage("Unable to instantiate Ibm MQ Processor. Is the \"com.ibm.mq.allclient.jar\" file added to the lib directory?", e);
			}
		}
	}
	
	
	private void initializeMetricReporter(MetricRegistry metricRegistry, Configuration configuration) {
		this.metricReporter = new MetricReporterElasticImpl(metricRegistry, configuration.instanceName, this.elasticClient);
		this.metricReporter.start(1, TimeUnit.MINUTES);
	}
	
    private class NamedThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        NamedThreadFactory(String name) {
            SecurityManager s = System.getSecurityManager();
            this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            this.namePrefix = name + "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(this.group, r, this.namePrefix + this.threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
            	t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

}
