/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.gui.rest.services.iib.proxy.v9;

import com.ibm.broker.config.proxy.*;
import com.jecstar.etm.gui.rest.services.iib.Node;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBIntegrationServer;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBNodeConnection;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@SuppressWarnings("deprecation")
public class IIBNodeConnectionV9Impl implements IIBNodeConnection {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(IIBNodeConnectionV9Impl.class);


    private BrokerProxy brokerProxy;
    private final Node node;

    public IIBNodeConnectionV9Impl(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    public void connect() {
        MQBrokerConnectionParameters bcp = new MQBrokerConnectionParameters(this.node.getHost(), this.node.getPort(), this.node.getQueueManager());
        if (this.node.getUsername() != null) {
            bcp.setUserID(this.node.getUsername());
        }
        if (this.node.getPassword() != null) {
            bcp.setPassword(this.node.getPassword());
        }
        if (this.node.getChannel() != null) {
            bcp.setAdvancedConnectionParameters(this.node.getChannel(), null, null, -1, -1, null);
        }
        try {
            String message = "Connecting to the integration node running at " + this.node.getHost() + ":" + this.node.getPort() + " with queuemanager '" + this.node.getQueueManager() + "'";
            if (this.node.getChannel() != null) {
                message += " and channel '" + this.node.getChannel() + "'.";
            } else {
                message += ".";
            }
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage(message);
            }
            this.brokerProxy = BrokerProxy.getInstance(bcp);

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
            return new IIBIntegrationServerV9Impl(this.brokerProxy.getExecutionGroupByName(serverName));
        } catch (ConfigManagerProxyPropertyNotInitializedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    public List<IIBIntegrationServer> getServers() {
        try {
            List<IIBIntegrationServer> servers = new ArrayList<>();
            Enumeration<ExecutionGroupProxy> executionGroups = this.brokerProxy.getExecutionGroups(null);
            while (executionGroups.hasMoreElements()) {
                servers.add(new IIBIntegrationServerV9Impl(executionGroups.nextElement()));
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
