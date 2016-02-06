package com.jecstar.etm.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.launcher.http.HttpServer;
import com.jecstar.etm.processor.elastic.ElasticBackedEtmConfiguration;
import com.jecstar.etm.processor.elastic.PersistenceEnvironmentElasticImpl;
import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;

public class Launcher {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(Launcher.class);

	private TelemetryCommandProcessor processor;
	private HttpServer httpServer;
	private Node node;
	private Client elasticClient;

	
	public void launch(CommandLineParameters commandLineParameters) {
		addShutdownHooks();
		try {
			final File configDir = new File(commandLineParameters.getConfigDirectory());
			final Configuration configuration = loadConfiguration(configDir);
			if (configuration.isProcessorNecessary() || configuration.guiEnabled) {
				initializeElasticsearchClient(configuration);
			}
			if (configuration.isProcessorNecessary()) {
				initializeProcessor(configuration);
			}
			if (configuration.isHttpServerNecessary()) {
				this.httpServer = new HttpServer(configDir, configuration, this.processor, this.elasticClient);
				this.httpServer.start();
			}
			if (log.isInfoLevelEnabled()) {
				log.logInfoMessage("Enterprise Telemetry Monitor started.");
			}
		} catch (FileNotFoundException e) {
			if (log.isFatalLevelEnabled()) {
				log.logFatalMessage("Error reading configuration file", e);
			}
		} catch (YamlException e) {
			if (log.isFatalLevelEnabled()) {
				log.logFatalMessage("Error parsing configuration file", e);
			}
		} catch (Exception e) {
			if (log.isFatalLevelEnabled()) {
				log.logFatalMessage("Error launching Enterprise Telemetry Monitor", e);
			}
		}		
	}
	
	private void addShutdownHooks() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (Launcher.this.httpServer != null) {
					try { Launcher.this.httpServer.stop(); } catch (Throwable t) {}
				}
				if (Launcher.this.processor != null) {
					try { Launcher.this.processor.stopAll(); } catch (Throwable t) {}
				}
				if (Launcher.this.elasticClient != null) {
					try { Launcher.this.elasticClient.close(); } catch (Throwable t) {}
				}
				if (Launcher.this.node != null) {
					try { Launcher.this.node.close(); } catch (Throwable t) {}
				}
			}
		});
	}
	
	private void initializeProcessor(Configuration configuration) {
		if (this.processor == null) {
			ExecutorService executor = Executors.newCachedThreadPool();
			this.processor = new TelemetryCommandProcessor();
			EtmConfiguration etmConfiguration = new ElasticBackedEtmConfiguration(configuration.instanceName, "processor", this.elasticClient);
			this.processor.start(executor, new PersistenceEnvironmentElasticImpl(etmConfiguration, this.elasticClient), etmConfiguration);
		}
	}
	
	private void initializeElasticsearchClient(Configuration configuration) {
		if (this.elasticClient != null) {
			return;
		}
		if (configuration.elasticsearch.connectAsNode) {
			if (this.node == null) {
				Builder settingsBuilder = Settings.settingsBuilder()
					.put("cluster.name", configuration.clusterName)
					.put("node.name", configuration.instanceName)
					.put("path.home", configuration.elasticsearch.nodeHomePath)
					.put("path.data", configuration.elasticsearch.nodeDataPath)
					.put("path.logs", configuration.elasticsearch.nodeLogPath)
					.put("http.enabled", false);
				if (configuration.getElasticsearchTransportPort() > 0) {
					settingsBuilder.put("transport.tcp.port", configuration.getElasticsearchTransportPort());
				}
				if (!configuration.elasticsearch.nodeMulticast) {
					settingsBuilder.put("discovery.zen.ping.multicast.enabled", false);
					settingsBuilder.put("discovery.zen.ping.unicast.hosts", configuration.elasticsearch.connectAddresses);
				}
				this.node = new NodeBuilder()
						.settings(settingsBuilder)
						.client(!configuration.elasticsearch.nodeData)
						.data(configuration.elasticsearch.nodeData)
						.clusterName(configuration.clusterName)
						.node();
			}
			this.elasticClient = node.client();
		} else {
			TransportClient transportClient = TransportClient.builder().settings(Settings.settingsBuilder()
					.put("cluster.name", configuration.clusterName)
					.put("client.transport.sniff", true)).build();
			String[] hosts = configuration.elasticsearch.connectAddresses.split(",");
			for (String host : hosts) {
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
			this.elasticClient = transportClient;
		}
	}

	private Configuration loadConfiguration(File configDir) throws FileNotFoundException, YamlException {
		if (!configDir.exists()) {
			Configuration configuration = new Configuration();
			return configuration;
		}
		File configFile = new File(configDir, "etm.yml");
		if (!configFile.exists() || !configFile.isFile() || !configFile.canRead()) {
			Configuration configuration = new Configuration();
			return configuration;
		}
		YamlReader reader = new YamlReader(new FileReader(configFile));
		reader.getConfig().setBeanProperties(false);
		return reader.read(Configuration.class);
	}
}
