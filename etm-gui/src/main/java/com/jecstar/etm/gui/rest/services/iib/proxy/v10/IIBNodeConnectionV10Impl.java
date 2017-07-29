package com.jecstar.etm.gui.rest.services.iib.proxy.v10;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.ibm.broker.config.proxy.BrokerProxy;
import com.ibm.broker.config.proxy.ConfigManagerProxyException;
import com.ibm.broker.config.proxy.ConfigManagerProxyLoggedException;
import com.ibm.broker.config.proxy.ConfigManagerProxyPropertyNotInitializedException;
import com.ibm.broker.config.proxy.ConfigurableService;
import com.ibm.broker.config.proxy.ExecutionGroupProxy;
import com.ibm.broker.config.proxy.IntegrationNodeConnectionParameters;
import com.jecstar.etm.gui.rest.services.iib.Node;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBIntegrationServer;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBNodeConnection;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

public class IIBNodeConnectionV10Impl implements IIBNodeConnection {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(IIBNodeConnectionV10Impl.class);


	private BrokerProxy brokerProxy;
	private final Node node;
	
	private IIBNodeConnectionV10Impl(Node node) {
		this.node = node;
	}
	
	public Node getNode() {
		return node;
	}

	public void connect() {
		IntegrationNodeConnectionParameters incp = new IntegrationNodeConnectionParameters(this.node.getHost(), this.node.getPort());
		if (this.node.getUsername() != null) {
			incp.setUserID(this.node.getUsername());
		}
		if (this.node.getPassword() != null) {
			incp.setPassword(this.node.getPassword());
		}
		// TODO support for ssl.
		try {
			if (log.isDebugLevelEnabled()) {
				log.logDebugMessage("Connecting to the integration node running at " + this.node.getHost() + ":" + this.node.getPort() + ".");
			}
			this.brokerProxy = BrokerProxy.getInstance(incp);
			if (!this.brokerProxy.hasBeenPopulatedByBroker(true)) {
				if (log.isWarningLevelEnabled()) { 
					log.logWarningMessage("Integration node '" + this.node.getHost() + ":" + this.node.getPort() + "' is not responding.");
				}
				throw new EtmException(EtmException.IIB_CONNECTION_ERROR);
			}
		} catch (ConfigManagerProxyException e) {
			if (log.isErrorLevelEnabled()) { 
				log.logErrorMessage("Unable to connect to integration node '" + this.node.getHost() + ":" + this.node.getPort() + "'.", e);
			}
			throw new EtmException(EtmException.IIB_CONNECTION_ERROR, e);
		}
	}
	
	public IIBIntegrationServer getServerByName(String serverName) {
		try {
			return new IIBIntegrationServerV10Impl(this.brokerProxy.getExecutionGroupByName(serverName));
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
	
	public List<IIBIntegrationServer> getServers() {
		try {
			List<IIBIntegrationServer> servers = new ArrayList<>();
			Enumeration<ExecutionGroupProxy> executionGroups = this.brokerProxy.getExecutionGroups(null);
			while (executionGroups.hasMoreElements()) {
				servers.add(new IIBIntegrationServerV10Impl(executionGroups.nextElement()));
			}
			return servers;
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}

	public void setSynchronous(int timeout) {
		this.brokerProxy.setSynchronous(timeout);
	}

	public ConfigurableService getConfigurableService(String type, String name) {
		try {
			return this.brokerProxy.getConfigurableService(type, name);
		} catch (ConfigManagerProxyPropertyNotInitializedException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}

	public void createConfigurableService(String type, String name) {
		try {
			this.brokerProxy.createConfigurableService(type, name);
		} catch (ConfigManagerProxyLoggedException | IllegalArgumentException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}
	
	public void deleteConfigurableService(String type, String name) {
		try {
			this.brokerProxy.deleteConfigurableService(type, name);
		} catch (ConfigManagerProxyLoggedException | IllegalArgumentException e) {
			throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
		}
	}

	@Override
	public void close() {
		if (this.brokerProxy != null) {
			this.brokerProxy.disconnect();
		}
	}

}
