package com.jecstar.etm.launcher;

import static io.undertow.servlet.Servlets.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletException;

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;

import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;
import com.jecstar.etm.processor.rest.RestTelemetryEventProcessorApplication;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;

public class HttpServer {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(HttpServer.class);

	private Configuration configuration;
	private Undertow server;
	private GracefulShutdownHandler shutdownHandler;
	private boolean started;

	public HttpServer(Configuration configuration, TelemetryCommandProcessor processor) {
		this.configuration = configuration;
		final PathHandler root = Handlers.path();
		this.shutdownHandler = Handlers.gracefulShutdown(root);
		final ServletContainer container = ServletContainer.Factory.newInstance();
		Builder builder = Undertow.builder();
		if (this.configuration.getHttpPort() > 0) {
			builder.addHttpListener(this.configuration.getHttpPort(), this.configuration.bindingAddress);
		}
		if (this.configuration.getHttpsPort() > 0) {
			if (this.configuration.sslKeystoreLocation == null) {
				log.logErrorMessage("SSL keystore not provided. Https listener not started.");
			} else {
				SSLContext sslContext;
				try {
					sslContext = createSslContext(this.configuration);
					builder.addHttpsListener(this.configuration.getHttpsPort(), this.configuration.bindingAddress, sslContext);
				} catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | CertificateException | KeyStoreException | IOException e) {
					log.logErrorMessage("Unable to create SSL context. Https listener not started.", e);
				}
			}
		}
		this.server = builder.setHandler(root).build();
		if (this.configuration.restProcessorEnabled) {
			RestTelemetryEventProcessorApplication processorApplication = new RestTelemetryEventProcessorApplication(
					processor);
			ResteasyDeployment deployment = new ResteasyDeployment();
			deployment.setApplication(processorApplication);
			DeploymentInfo di = undertowRestDeployment(deployment, "/");
			di.setClassLoader(processorApplication.getClass().getClassLoader());
			di.setContextPath("/rest/processor/");
			di.setDeploymentName("Rest event processor - " + di.getContextPath());
			DeploymentManager manager = container.addDeployment(di);
			manager.deploy();
			try {
				root.addPrefixPath(di.getContextPath(), manager.start());
			} catch (ServletException e) {
				log.logErrorMessage("Error deploying rest processor", e);
			}
		}
	}

	private SSLContext createSslContext(Configuration configuration) throws KeyManagementException, NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException, UnrecoverableKeyException {
		KeyStore keyStore = loadKeyStore(configuration.sslKeystoreLocation, configuration.sslKeystoreType, configuration.sslKeystorePassword);
		KeyStore trustStore = loadKeyStore(configuration.sslTruststoreLocation, configuration.sslTruststoreType, configuration.sslTruststorePassword);
		KeyManager[] keyManagers = buildKeyManagers(keyStore, configuration.sslKeystorePassword);
		TrustManager[] trustManagers = buildTrustManagers(configuration.sslTruststoreLocation == null ? null : trustStore, configuration.sslTruststorePassword);
		if (keyManagers == null || trustManagers == null) {
			return null;
		}
		SSLContext sslContext = SSLContext.getInstance(configuration.sslProtocol);
		sslContext.init(keyManagers, trustManagers, null);
		return sslContext;
	}

	private KeyStore loadKeyStore(final File location, String type, final String storePassword)
			throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
		try (final InputStream stream = location == null ? null : new FileInputStream(location);) {
			KeyStore loadedKeystore = KeyStore.getInstance(type);
			loadedKeystore.load(stream, storePassword == null ? null : storePassword.toCharArray());
			return loadedKeystore;
		}
	}

	private KeyManager[] buildKeyManagers(final KeyStore keyStore, final String storePassword) throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keyStore, storePassword == null ? null : storePassword.toCharArray());
		return keyManagerFactory.getKeyManagers();
	}

	private TrustManager[] buildTrustManagers(final KeyStore trustStore, final String storePassword) throws KeyStoreException, NoSuchAlgorithmException {
		if (trustStore == null) {
			return new TrustManager[] { new TrustAllTrustManager() };
		}
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(trustStore);
		return trustManagerFactory.getTrustManagers();
	}

	public void start() {
		if (!this.started) {
			this.server.start();
			this.started = true;
		}
	}

	public void stop() {
		if (this.shutdownHandler != null) {
			this.shutdownHandler.shutdown();
			try {
				this.shutdownHandler.awaitShutdown(30000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		if (this.server != null && this.started) {
			this.server.stop();
			this.started = false;
		}
	}

	private static DeploymentInfo undertowRestDeployment(ResteasyDeployment deployment, String mapping) {
		if (mapping == null) {
			mapping = "/";
		}
		if (!mapping.startsWith("/")) {
			mapping = "/" + mapping;
		}
		if (!mapping.endsWith("/")) {
			mapping += "/";
		}
		mapping = mapping + "*";
		String prefix = null;
		if (!mapping.equals("/*")) {
			prefix = mapping.substring(0, mapping.length() - 2);
		}
		ServletInfo resteasyServlet = servlet("ResteasyServlet", HttpServletDispatcher.class).setAsyncSupported(true)
				.setLoadOnStartup(1).addMapping(mapping);
		if (prefix != null) {
			resteasyServlet.addInitParam("resteasy.servlet.mapping.prefix", prefix);
		}
		return new DeploymentInfo().addServletContextAttribute(ResteasyDeployment.class.getName(), deployment)
				.addServlet(resteasyServlet);
	}

	private final class TrustAllTrustManager implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

	}

}
