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

package com.jecstar.etm.server.core.domain.converter.json;

import com.jecstar.etm.domain.*;
import com.jecstar.etm.domain.writer.TelemetryEventTags;
import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.domain.writer.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.server.core.domain.converter.PayloadDecoder;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

/**
 * Helper class for all event converters.
 *
 * @param <Event>
 */
class TelemetryEventJsonConverter<Event extends TelemetryEvent<Event>> extends JsonConverter {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(TelemetryEventJsonConverter.class);

    private final TelemetryEventTags tags = new TelemetryEventTagsJsonImpl();
    private final PayloadDecoder payloadDecoder = new PayloadDecoder();

    public void convert(Map<String, Object> valueMap, Event telemetryEvent, String id) {
        telemetryEvent.initialize();
        telemetryEvent.id = id;
        if (telemetryEvent.id != null && telemetryEvent.id.trim().length() == 0) {
            telemetryEvent.id = null;
        }
        telemetryEvent.correlationId = getString(this.tags.getCorrelationIdTag(), valueMap);
        if (telemetryEvent.correlationId != null && telemetryEvent.correlationId.trim().length() == 0) {
            telemetryEvent.correlationId = null;
        }
        Map<String, Object> eventMap = getObject(this.tags.getCorrelationDataTag(), valueMap);
        List<String> correlations = getArray(this.tags.getCorrelationsTag(), valueMap);
        if (correlations != null && !correlations.isEmpty()) {
            telemetryEvent.correlations.addAll(correlations);
        }
        if (eventMap != null && !eventMap.isEmpty()) {
            telemetryEvent.correlationData.putAll(eventMap);
        }
        List<Map<String, Object>> endpoints = getArray(this.tags.getEndpointsTag(), valueMap);
        if (endpoints != null) {
            for (Map<String, Object> endpointMap : endpoints) {
                Endpoint endpoint = new Endpoint();
                endpoint.name = getString(this.tags.getEndpointNameTag(), endpointMap);
                List<Map<String, Object>> endpointHandlers = getArray(this.tags.getEndpointHandlersTag(), endpointMap);
                if (endpointHandlers != null) {
                    for (Map<String, Object> eh : endpointHandlers) {
                        EndpointHandler endpointHandler = createEndpointFormValueMapHandler(eh);
                        if (endpointHandler != null) {
                            endpoint.addEndpointHandler(endpointHandler);
                        }
                    }
                }
                telemetryEvent.endpoints.add(endpoint);
            }
        }
        eventMap = getObject(this.tags.getExtractedDataTag(), valueMap);
        if (eventMap != null && !eventMap.isEmpty()) {
            telemetryEvent.extractedData.putAll(eventMap);
        }
        telemetryEvent.name = getString(this.tags.getNameTag(), valueMap);
        eventMap = getObject(this.tags.getMetadataTag(), valueMap);
        if (eventMap != null && !eventMap.isEmpty()) {
            telemetryEvent.metadata.putAll(eventMap);
        }
        telemetryEvent.payload = this.payloadDecoder.decode(getString(this.tags.getPayloadTag(), valueMap), PayloadEncoding.safeValueOf(getString(this.tags.getPayloadEncodingTag(), valueMap)));
        telemetryEvent.payloadFormat = PayloadFormat.safeValueOf(getString(this.tags.getPayloadFormatTag(), valueMap));
    }

    private EndpointHandler createEndpointFormValueMapHandler(Map<String, Object> valueMap) {
        if (valueMap.isEmpty()) {
            return null;
        }
        EndpointHandler endpointHandler = new EndpointHandler();
        Map<String, Object> applicationValueMap = getObject(this.tags.getEndpointHandlerApplicationTag(), valueMap);
        if (applicationValueMap != null && !applicationValueMap.isEmpty()) {
            String stringHostAddress = getString(this.tags.getApplicationHostAddressTag(), applicationValueMap);
            String hostName = getString(this.tags.getApplicationHostNameTag(), applicationValueMap);
            if (stringHostAddress != null) {
                byte[] address = null;
                try {
                    address = InetAddress.getByName(stringHostAddress).getAddress();
                } catch (UnknownHostException e) {
                    if (log.isDebugLevelEnabled()) {
                        log.logDebugMessage(e.getMessage(), e);
                    }
                }
                if (address != null) {
                    if (hostName != null) {
                        try {
                            endpointHandler.application.hostAddress = InetAddress.getByAddress(hostName, address);
                        } catch (UnknownHostException e) {
                            if (log.isDebugLevelEnabled()) {
                                log.logDebugMessage(e.getMessage(), e);
                            }
                        }
                    } else {
                        try {
                            endpointHandler.application.hostAddress = InetAddress.getByAddress(address);
                        } catch (UnknownHostException e) {
                            if (log.isDebugLevelEnabled()) {
                                log.logDebugMessage(e.getMessage(), e);
                            }
                        }
                    }
                }
            } else if (hostName != null) {
                try {
                    endpointHandler.application.hostAddress = InetAddress.getByName(hostName);
                } catch (UnknownHostException e) {
                    if (log.isDebugLevelEnabled()) {
                        log.logDebugMessage(e.getMessage(), e);
                    }
                }
            }
            endpointHandler.application.instance = getString(this.tags.getApplicationInstanceTag(), applicationValueMap);
            endpointHandler.application.name = getString(this.tags.getApplicationNameTag(), applicationValueMap);
            endpointHandler.application.principal = getString(this.tags.getApplicationPrincipalTag(), applicationValueMap);
            endpointHandler.application.version = getString(this.tags.getApplicationVersionTag(), applicationValueMap);
        }
        endpointHandler.type = EndpointHandler.EndpointHandlerType.safeValueOf(getString(this.tags.getEndpointHandlerTypeTag(), valueMap));
        endpointHandler.handlingTime = getInstant(this.tags.getEndpointHandlerHandlingTimeTag(), valueMap);
        endpointHandler.latency = getLong(this.tags.getEndpointHandlerLatencyTag(), valueMap);
        endpointHandler.responseTime = getLong(this.tags.getEndpointHandlerResponseTimeTag(), valueMap);
        endpointHandler.transactionId = getString(this.tags.getEndpointHandlerTransactionIdTag(), valueMap);
        if (endpointHandler.transactionId != null && endpointHandler.transactionId.trim().length() == 0) {
            endpointHandler.transactionId = null;
        }
        endpointHandler.sequenceNumber = getInteger(this.tags.getEndpointHandlerSequenceNumberTag(), valueMap);
        Map<String, Object> locationValueMap = getObject(this.tags.getEndpointHandlerLocationTag(), valueMap);
        if (locationValueMap != null && !locationValueMap.isEmpty()) {
            endpointHandler.location.latitude = getDouble(this.tags.getLatitudeTag(), locationValueMap);
            endpointHandler.location.longitude = getDouble(this.tags.getLongitudeTag(), locationValueMap);
        }
        Map<String, Object> metaDataMap = getObject(this.tags.getMetadataTag(), valueMap);
        if (metaDataMap != null && !metaDataMap.isEmpty()) {
            endpointHandler.metadata.putAll(metaDataMap);
        }
        return endpointHandler;
    }

    void addDatabaseFields(Event event, JsonBuilder builder) {
        builder.field(this.tags.getTimestampTag(), System.currentTimeMillis());
        if (event.id != null) {
            builder.field(this.tags.getEventHashesTag(), new Long[]{event.getCalculatedHash()});
        }
    }


}
