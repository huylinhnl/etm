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

import com.ibm.broker.config.proxy.ConfigManagerProxyPropertyNotInitializedException;
import com.ibm.broker.config.proxy.LibraryProxy;
import com.ibm.broker.config.proxy.MessageFlowProxy;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBLibrary;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBMessageFlow;
import com.jecstar.etm.gui.rest.services.iib.proxy.IIBSubFlow;
import com.jecstar.etm.server.core.EtmException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class IIBLibraryV9Impl implements IIBLibrary {

    private final LibraryProxy library;

    IIBLibraryV9Impl(LibraryProxy libraryProxy) {
        this.library = libraryProxy;
    }

    public String getName() {
        try {
            return this.library.getName();
        } catch (ConfigManagerProxyPropertyNotInitializedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    public List<IIBMessageFlow> getMessageFlows() {
        try {
            List<IIBMessageFlow> messageFlows = new ArrayList<>();
            Enumeration<MessageFlowProxy> messageFlowProxies = this.library.getMessageFlows(null);
            while (messageFlowProxies.hasMoreElements()) {
                messageFlows.add(new IIBMessageFlow(messageFlowProxies.nextElement()));
            }
            return messageFlows;
        } catch (ConfigManagerProxyPropertyNotInitializedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    public IIBMessageFlow getMessageFlowByName(String flowName) {
        try {
            MessageFlowProxy messageFlowProxy = this.library.getMessageFlowByName(flowName);
            if (messageFlowProxy == null) {
                return null;
            }
            return new IIBMessageFlow(messageFlowProxy);
        } catch (ConfigManagerProxyPropertyNotInitializedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }

    public List<IIBSubFlow> getSubFlows() {
        return Collections.emptyList();
    }


    public IIBSubFlow getSubFlowByName(String subFlowName) {
        return null;
    }

    public String getVersion() {
        try {
            return this.library.getVersion();
        } catch (ConfigManagerProxyPropertyNotInitializedException e) {
            throw new EtmException(EtmException.WRAPPED_EXCEPTION, e);
        }
    }
}
