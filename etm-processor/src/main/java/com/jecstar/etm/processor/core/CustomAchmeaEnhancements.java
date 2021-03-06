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

package com.jecstar.etm.processor.core;

import com.jecstar.etm.domain.MessagingTelemetryEvent;
import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.domain.parser.XPathExpressionParser;

import java.util.UUID;

class CustomAchmeaEnhancements {

    private final EtmConfiguration etmConfiguration;

    private final XPathExpressionParser ibfExpression;
    private final XPathExpressionParser oudAchmeaExpression;
    private final XPathExpressionParser soapBodyExpression;

    CustomAchmeaEnhancements(EtmConfiguration etmConfiguration) {
        this.etmConfiguration = etmConfiguration;
        this.ibfExpression = new XPathExpressionParser("custom-achmea-ibf-type", "/*[local-name()='Envelope']/*[local-name()='Header']/*[local-name()='IBFheader']/*[local-name()='MessageType']");
        this.oudAchmeaExpression = new XPathExpressionParser("custom-achmea-oax-type", "/*/*[local-name()='Header']/*[local-name()='Berichttype']/*[local-name()='identificatie']");
        this.soapBodyExpression = new XPathExpressionParser("custom-achmea-soap-type", "local-name(/*[local-name()='Envelope']/*[local-name()='Body']/*)");
    }

    public void enhanceMessagingEvent(MessagingTelemetryEvent event) {
        String companyName = this.etmConfiguration.getLicense().getOwner();
        if (!companyName.startsWith("Achmea")) {
            return;
        }
        // First determine the message type. At achmea messages are cloned and are all datagram message. Try to determine the type based on the payload.
        if (MessagingEventType.FIRE_FORGET.equals(event.messagingEventType) && event.payload != null) {
            String ibfType = this.ibfExpression.evaluate(event.payload);
            if (ibfType != null && ibfType.trim().length() > 0) {
                ibfType = ibfType.trim();
                if ("request".equalsIgnoreCase(ibfType)) {
                    event.messagingEventType = MessagingEventType.REQUEST;
                    return;
                } else if ("response".equalsIgnoreCase(ibfType)) {
                    event.messagingEventType = MessagingEventType.RESPONSE;
                    return;
                } else if ("datagram".equalsIgnoreCase(ibfType)) {
                    event.messagingEventType = MessagingEventType.FIRE_FORGET;
                    return;
                }
            }

            String oudAchmeaIdentificatie = this.oudAchmeaExpression.evaluate(event.payload);
            if (oudAchmeaIdentificatie != null && oudAchmeaIdentificatie.trim().length() > 0) {
                oudAchmeaIdentificatie = oudAchmeaIdentificatie.trim().toLowerCase();
                if (oudAchmeaIdentificatie.endsWith("v")) {
                    event.messagingEventType = MessagingEventType.REQUEST;
                    return;
                } else if (oudAchmeaIdentificatie.endsWith("a")) {
                    event.messagingEventType = MessagingEventType.RESPONSE;
                    return;
                }
            }

            String soapBodyChild = this.soapBodyExpression.evaluate(event.payload);
            if (soapBodyChild != null && soapBodyChild.trim().length() > 0) {
                soapBodyChild = soapBodyChild.trim().toLowerCase();
                if (soapBodyChild.endsWith("request")) {
                    event.messagingEventType = MessagingEventType.REQUEST;
                    return;
                } else if (soapBodyChild.endsWith("response")) {
                    event.messagingEventType = MessagingEventType.RESPONSE;
                    return;
                } else if (soapBodyChild.equals("fault")) {
                    event.messagingEventType = MessagingEventType.RESPONSE;
                    return;
                }
            }
        }
        // In case of an error a message with an reused message id is generated. This overwrites the original message so we change the id's over here.
        if (event.payload != null && event.payload.trim().startsWith("WmbException")) {
            event.correlationId = event.id;
            // Reassign a new unique id.
            event.id = UUID.randomUUID().toString();
        }
    }
}
