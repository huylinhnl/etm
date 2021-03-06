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

package com.jecstar.etm.integration.test.core;

import com.consol.citrus.exceptions.CitrusRuntimeException;
import org.snmp4j.*;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.AbstractTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;

import java.io.IOException;

/**
 * A super simple Snmp PDU receiver.
 */
public class EmbeddableSnmpReceiver implements CommandResponder {

    public static final String HOST = "127.0.0.1";
    public static final int PORT = 10162;
    public static final String COMMUNITY = "public";
    private Snmp snmp;
    private PDU pdu;


    public void startServer() {
        try {
            AbstractTransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping(new UdpAddress(HOST + "/" + PORT));

            ThreadPool threadPool = ThreadPool.create("DispatcherPool", 3);
            MessageDispatcher dispatcher = new MultiThreadedMessageDispatcher(
                    threadPool, new MessageDispatcherImpl());

            // add message processing models
            dispatcher.addMessageProcessingModel(new MPv1());
            dispatcher.addMessageProcessingModel(new MPv2c());

            // add all security protocols
            SecurityProtocols.getInstance().addDefaultProtocols();

            // Create Target
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString(COMMUNITY));

            this.snmp = new Snmp(dispatcher, transport);
            this.snmp.addCommandResponder(this);

            transport.listen();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopServer() {
        try {
            this.snmp.close();
        } catch (IOException e) {
        }
    }

    /**
     * This method will be called whenever a pdu is received on the given port
     * specified in the listen() method
     */
    public void processPdu(CommandResponderEvent cmdRespEvent) {
        this.pdu = cmdRespEvent.getPDU();
    }

    public PDU retrievePDU(int timeout) {
        long starTime = System.currentTimeMillis();
        while (this.pdu == null) {
            if (System.currentTimeMillis() - starTime > timeout) {
                throw new CitrusRuntimeException("Timeout waiting for PDU");
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return this.pdu;
    }
}