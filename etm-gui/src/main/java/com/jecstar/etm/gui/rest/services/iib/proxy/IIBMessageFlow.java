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

package com.jecstar.etm.gui.rest.services.iib.proxy;

import com.ibm.broker.config.proxy.ConfigManagerProxyLoggedException;
import com.ibm.broker.config.proxy.ConfigManagerProxyPropertyNotInitializedException;
import com.ibm.broker.config.proxy.MessageFlowProxy;
import com.ibm.broker.config.proxy.MessageFlowProxy.Node;
import com.jecstar.etm.server.core.EtmException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class IIBMessageFlow {

    private static final String RUNTIME_PROPERTY_MONITORING = "This/monitoring";
    private static final String RUNTIME_PROPERTY_MONITORING_PROFILE = "This/monitoringProfile";

    private final MessageFlowProxy messageFlow;

    public IIBMessageFlow(MessageFlowProxy messageFlowProxy) {
        this.messageFlow = messageFlowProxy;
    }

    public String getName() {
        try {
            return this.messageFlow.getName();
        } catch (ConfigManagerProxyPropertyNotInitializedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    public boolean isMonitoringActivated() {
        try {
            if (this.messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING) != null
                    && this.messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING)
                    .equals("active")) {
                return true;
            }
        } catch (ConfigManagerProxyPropertyNotInitializedException | IllegalArgumentException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
        return false;
    }

    public String getMonitoringProfileName() {
        try {
            return this.messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING_PROFILE);
        } catch (ConfigManagerProxyPropertyNotInitializedException | IllegalArgumentException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    public List<IIBNode> getNodes() {
        try {
            List<IIBNode> nodes = new ArrayList<>();
            Enumeration<Node> nodeProxy = this.messageFlow.getNodes();
            while (nodeProxy.hasMoreElements()) {
                IIBNode iibNode = new IIBNode(nodeProxy.nextElement());
                if (nodes.stream().noneMatch(n -> n.getName().equals(iibNode.getName()))) {
                    // Filter nodes with the same name. Although it should not
                    // be possible to have 2 nodes with the same name in a
                    // single IIB flow it might be the case when a Publication
                    // node is used in a flow. A single Publication node in a
                    // flow will result in 2 unique Nodes in the xml of the
                    // flow. Both nodes have the exact same name, but a
                    // different uuid.
                    nodes.add(iibNode);
                }
            }
            return nodes;
        } catch (ConfigManagerProxyPropertyNotInitializedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    public String getVersion() {
        try {
            return this.messageFlow.getVersion();
        } catch (ConfigManagerProxyPropertyNotInitializedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    public void activateMonitoringProfile(String monitoringProfileName) {
        try {
            String currenProfileName = this.messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING_PROFILE);
            if (currenProfileName == null || !currenProfileName.equals(monitoringProfileName)) {
                this.messageFlow.setRuntimeProperty(RUNTIME_PROPERTY_MONITORING_PROFILE, monitoringProfileName);
            }
            String status = this.messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING);
            if (status == null || !status.equals("active")) {
                this.messageFlow.setRuntimeProperty(RUNTIME_PROPERTY_MONITORING, "active");
            }
        } catch (ConfigManagerProxyPropertyNotInitializedException | IllegalArgumentException | ConfigManagerProxyLoggedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    public String deactivateMonitoringProfile() {
        try {
            String status = messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING);
            if ("active".equals(status)) {
                this.messageFlow.setRuntimeProperty(RUNTIME_PROPERTY_MONITORING, "inactive");
            }
            String currentProfile = messageFlow.getRuntimeProperty(RUNTIME_PROPERTY_MONITORING_PROFILE);
            if (currentProfile != null) {
                this.messageFlow.setRuntimeProperty(RUNTIME_PROPERTY_MONITORING_PROFILE, "");
            }
            return currentProfile;
        } catch (ConfigManagerProxyPropertyNotInitializedException | IllegalArgumentException | ConfigManagerProxyLoggedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }
}
