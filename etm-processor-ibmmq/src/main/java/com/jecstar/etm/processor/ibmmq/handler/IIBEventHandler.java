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

package com.jecstar.etm.processor.ibmmq.handler;

import com.ibm.mq.MQMessage;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.*;
import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.HttpTelemetryEvent.HttpEventType;
import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.builder.*;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.handler.HandlerResult;
import com.jecstar.etm.processor.handler.HandlerResults;
import com.jecstar.etm.processor.ibmmq.event.ApplicationData.ComplexContent;
import com.jecstar.etm.processor.ibmmq.event.ApplicationData.SimpleContent;
import com.jecstar.etm.processor.ibmmq.event.EncodingType;
import com.jecstar.etm.processor.ibmmq.event.Event;
import com.jecstar.etm.processor.ibmmq.event.SimpleContentDataType;
import com.jecstar.etm.server.core.EtmException;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.TimeZone;

public class IIBEventHandler extends AbstractMQEventHandler {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(IIBEventHandler.class);

    private final TelemetryCommandProcessor telemetryCommandProcessor;

    private final StringBuilder byteArrayBuilder = new StringBuilder();
    private final Unmarshaller unmarshaller;
    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    private final HttpTelemetryEventBuilder httpTelemetryEventBuilder = new HttpTelemetryEventBuilder();
    private final MessagingTelemetryEventBuilder messagingTelemetryEventBuilder = new MessagingTelemetryEventBuilder();

    public IIBEventHandler(TelemetryCommandProcessor telemetryCommandProcessor, String defaultImportProfile) {
        super(defaultImportProfile);
        this.telemetryCommandProcessor = telemetryCommandProcessor;
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Event.class);
            this.unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            throw new EtmException(EtmException.UNMARSHALLER_CREATE_EXCEPTION, e);
        }
    }

    @Override
    protected TelemetryCommandProcessor getProcessor() {
        return this.telemetryCommandProcessor;
    }

    @SuppressWarnings("unchecked")
    public HandlerResults handleMessage(MQMessage message) {
        HandlerResults results = new HandlerResults();
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(getContent(message).getBytes()))) {
            Event event = ((JAXBElement<Event>) this.unmarshaller.unmarshal(reader)).getValue();
            results.addHandlerResult(process(message.messageId, event));
        } catch (JAXBException | IOException e) {
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("Unable to unmarshall event.", e);
            }
            results.addHandlerResult(HandlerResult.parserFailure(e));
        }
        return results;
    }

    private HandlerResult process(byte[] messageId, Event event) {
        // We use the event name field as it is the only field that can be set with hard values. All other fields can only be set with xpath values from the message payload.
        String nodeType = event.getEventPointData().getMessageFlowData().getNode().getNodeType();
        if (nodeType == null) {
            String message = "NodeType of event with id '" + byteArrayToString(messageId) + "' is null. Unable to determine event type. Event will not be processed.";
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage(message);
            }
            return HandlerResult.failed(message);
        }
        // See https://www.ibm.com/support/knowledgecenter/SSMKHH_10.0.0/com.ibm.etools.mft.doc/as36001_.htm for node types.
        if (nodeType.startsWith("ComIbmMQ") || "ComIbmPublicationNode".equals(nodeType)) {
            return processAsMessagingEvent(messageId, event);
        } else if ((nodeType.startsWith("ComIbmHTTP") && !nodeType.equals("ComIbmHTTPHeader")) ||
                nodeType.startsWith("ComIbmWS") ||
//				nodeType.startsWith("ComIbmREST") ||
                (nodeType.startsWith("ComIbmSOAP") && !nodeType.equals("ComIbmSOAPWrapperNode") && !nodeType.equals("ComIbmSOAPExtractNode"))) {
            return processAsHttpEvent(messageId, event);
        }
        String message = "Event with id '" + byteArrayToString(messageId) + "' has an unsupported NodeType '" + nodeType + "'. Event will not be processed.";
        if (log.isDebugLevelEnabled()) {
            log.logDebugMessage("Event with id '" + byteArrayToString(messageId) + "' has an unsupported NodeType '" + nodeType + "'. Event will not be processed.");
        }
        return HandlerResult.failed(message);
    }

    private HandlerResult processAsMessagingEvent(byte[] messageId, Event event) {
        this.messagingTelemetryEventBuilder.initialize();
        int encoding = -1;
        int ccsid = -1;
        String nodeType = event.getEventPointData().getMessageFlowData().getNode().getNodeType();
        EndpointBuilder endpointBuilder = new EndpointBuilder();
        // Determine the encoding & ccsid based on values from the event.
        if (event.getApplicationData() != null && event.getApplicationData().getSimpleContent() != null) {
            for (SimpleContent simpleContent : event.getApplicationData().getSimpleContent()) {
                if ("Encoding".equals(simpleContent.getName()) && SimpleContentDataType.INTEGER.equals(simpleContent.getDataType())) {
                    encoding = Integer.valueOf(simpleContent.getValue());
                } else if ("CodedCharSetId".equals(simpleContent.getName()) && SimpleContentDataType.INTEGER.equals(simpleContent.getDataType())) {
                    ccsid = Integer.valueOf(simpleContent.getValue());
                } else if ("Topic".equals(simpleContent.getName()) && SimpleContentDataType.STRING.equals(simpleContent.getDataType()) && "ComIbmPublicationNode".equals(nodeType)) {
                    endpointBuilder.setName(simpleContent.getValue());
                }
            }
        }
        // TODO, filteren op output terminal? Events op de in terminal van de MqOutputNode hebben nog geen msg id.
        if (event.getApplicationData() != null && event.getApplicationData().getComplexContent() != null) {
            for (ComplexContent complexContent : event.getApplicationData().getComplexContent()) {
                if (!("DestinationData".equals(complexContent.getElementName()))) {
                    continue;
                }
                NodeList nodeList = complexContent.getAny().getElementsByTagName("queueName");
                if (nodeList.getLength() > 0) {
                    endpointBuilder.setName(nodeList.item(0).getTextContent() != null ? nodeList.item(0).getTextContent().trim() : null);
                }
                nodeList = complexContent.getAny().getElementsByTagName("msgId");
                if (nodeList.getLength() > 0) {
                    this.messagingTelemetryEventBuilder.setId(nodeList.item(0).getTextContent());
                }
                nodeList = complexContent.getAny().getElementsByTagName("correlId");
                if (nodeList.getLength() > 0) {
                    String correlId = nodeList.item(0).getTextContent();
                    if (correlId.replaceAll("0", "").trim().length() != 0) {
                        this.messagingTelemetryEventBuilder.setCorrelationId(correlId);
                    }
                }
            }
        }
        // Add some flow information
        addCorrelationInformation(event, this.messagingTelemetryEventBuilder);
        EndpointHandlerBuilder endpointHandlerBuilder = createEndpointHandlerBuilder(event);
        if ("ComIbmMQInputNode".equals(nodeType) || "ComIbmMQGetNode".equals(nodeType)) {
            String endpoint = event.getEventPointData().getMessageFlowData().getNode().getDetail();
            if (endpointBuilder.getName() == null) {
                endpointBuilder.setName(endpoint);
            }
            endpointBuilder.addEndpointHandler(endpointHandlerBuilder.setType(EndpointHandler.EndpointHandlerType.READER));
        } else {
            endpointBuilder.addEndpointHandler(endpointHandlerBuilder.setType(EndpointHandler.EndpointHandlerType.WRITER));
        }
        this.messagingTelemetryEventBuilder.addOrMergeEndpoint(endpointBuilder);
        if (event.getBitstreamData() == null || event.getBitstreamData().getBitstream() == null) {
            String message = "Event with id '" + byteArrayToString(messageId) + "' has no bitstream. Event will not be processed.";
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage(message);
            }
            return HandlerResult.failed(message);
        }
        if (!EncodingType.BASE_64_BINARY.equals(event.getBitstreamData().getBitstream().getEncoding())) {
            String message = "Message with id '" + byteArrayToString(messageId)
                    + "' has an unsupported bitstream encoding type '"
                    + event.getBitstreamData().getBitstream().getEncoding().name() + "'. Use '"
                    + EncodingType.BASE_64_BINARY.name() + "' instead.";
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage(message);
            }
            return HandlerResult.failed(message);
        }
        byte[] decoded = Base64.getDecoder().decode(event.getBitstreamData().getBitstream().getValue());
        try {
            parseBitstreamAsMqMessage(event, decoded, this.messagingTelemetryEventBuilder, endpointHandlerBuilder, encoding, ccsid);
        } catch (MQDataException | IOException e) {
            String message = "Failed to parse MQ bitstream of event with id '" + byteArrayToString(messageId) + "'.";
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage(message, e);
            }
            return HandlerResult.failed(message);
        }
        this.telemetryCommandProcessor.processTelemetryEvent(this.messagingTelemetryEventBuilder, getDefaultImportProfile());
        return HandlerResult.processed();
    }

    private HandlerResult processAsHttpEvent(byte[] messageId, Event event) {
        this.httpTelemetryEventBuilder.initialize();
        int encoding = -1;
        int ccsid = -1;
        String httpIdentifier = null;
        // Determine the encoding & ccsid based on values from the event.
        if (event.getApplicationData() != null && event.getApplicationData().getSimpleContent() != null) {
            for (SimpleContent simpleContent : event.getApplicationData().getSimpleContent()) {
                if ("Encoding".equals(simpleContent.getName()) && SimpleContentDataType.INTEGER.equals(simpleContent.getDataType())) {
                    encoding = Integer.valueOf(simpleContent.getValue());
                } else if ("CodedCharSetId".equals(simpleContent.getName()) && SimpleContentDataType.INTEGER.equals(simpleContent.getDataType())) {
                    ccsid = Integer.valueOf(simpleContent.getValue());
                } else if ("ReplyIdentifier".equals(simpleContent.getName()) && SimpleContentDataType.HEX_BINARY.equals(simpleContent.getDataType())) {
                    httpIdentifier = simpleContent.getValue();
                } else if ("RequestIdentifier".equals(simpleContent.getName()) && SimpleContentDataType.HEX_BINARY.equals(simpleContent.getDataType())) {
                    httpIdentifier = simpleContent.getValue();
                }
            }
        }

        // TODO uitlezen local environment voor het id & correlationId.
        EndpointBuilder endpointBuilder = new EndpointBuilder();
        // Add some flow information
        addCorrelationInformation(event, this.httpTelemetryEventBuilder);

        EndpointHandlerBuilder endpointHandlerBuilder = createEndpointHandlerBuilder(event);
        String nodeType = event.getEventPointData().getMessageFlowData().getNode().getNodeType();
        if ("ComIbmHTTPAsyncResponse".equals(nodeType) ||
                "ComIbmWSInputNode".equals(nodeType) ||
                "ComIbmSOAPInputNode".equals(nodeType) ||
                "ComIbmSOAPAsyncResponseNode".equals(nodeType) ||
                "ComIbmRESTAsyncResponse".equals(nodeType)) {
            endpointBuilder.addEndpointHandler(endpointHandlerBuilder.setType(EndpointHandler.EndpointHandlerType.READER));
        } else {
            endpointBuilder.addEndpointHandler(endpointHandlerBuilder.setType(EndpointHandler.EndpointHandlerType.WRITER));
        }
        if ("ComIbmHTTPAsyncResponse".equals(nodeType) ||
                "ComIbmWSReplyNode".equals(nodeType) ||
                "ComIbmSOAPReplyNode".equals(nodeType) ||
                "ComIbmSOAPAsyncResponseNode".equals(nodeType) ||
                "ComIbmRESTAsyncResponse".equals(nodeType)) {
            this.httpTelemetryEventBuilder.setCorrelationId(httpIdentifier);
            this.httpTelemetryEventBuilder.setHttpEventType(HttpEventType.RESPONSE);
        } else {
            this.httpTelemetryEventBuilder.setId(httpIdentifier);
        }
        if (event.getBitstreamData() == null || event.getBitstreamData().getBitstream() == null) {
            String message = "Event with id '" + byteArrayToString(messageId) + "' has no bitstream. Event will not be processed.";
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage(message);
            }
            return HandlerResult.failed(message);
        }
        if (!EncodingType.BASE_64_BINARY.equals(event.getBitstreamData().getBitstream().getEncoding())) {
            String message = "Message with id '" + byteArrayToString(messageId)
                    + "' has an unsupported bitstream encoding type '"
                    + event.getBitstreamData().getBitstream().getEncoding().name() + "'. Use '"
                    + EncodingType.BASE_64_BINARY.name() + "' instead.";
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage(message);
            }
            return HandlerResult.failed(message);
        }
        byte[] decoded = Base64.getDecoder().decode(event.getBitstreamData().getBitstream().getValue());
        try {
            parseBitstreamAsHttpMessage(decoded, this.httpTelemetryEventBuilder, endpointBuilder, endpointHandlerBuilder, encoding, ccsid);
        } catch (UnsupportedEncodingException e) {
            String message = "Failed to parse HTTP bitstream of event with id '" + byteArrayToString(messageId) + "'.";
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage(message, e);
            }
            return HandlerResult.failed(message);
        }
        this.httpTelemetryEventBuilder.addOrMergeEndpoint(endpointBuilder);
        this.telemetryCommandProcessor.processTelemetryEvent(this.httpTelemetryEventBuilder, getDefaultImportProfile());
        return HandlerResult.processed();
    }

    private void addCorrelationInformation(Event event, TelemetryEventBuilder<?, ?> builder) {
        putNonNullDataInMap("IIB_LocalTransactionId", event.getEventPointData().getEventData().getEventCorrelation().getLocalTransactionId(), builder.getCorrelationData());
        putNonNullDataInMap("IIB_ParentTransactionId", event.getEventPointData().getEventData().getEventCorrelation().getParentTransactionId(), builder.getCorrelationData());
        putNonNullDataInMap("IIB_GlobalTransactionId", event.getEventPointData().getEventData().getEventCorrelation().getGlobalTransactionId(), builder.getCorrelationData());
    }

    private EndpointHandlerBuilder createEndpointHandlerBuilder(Event event) {
        // Try to parse some metadata that can be "hidden" in the event name.
        String eventData = event.getEventPointData().getEventData().getEventIdentity().getEventName();
        String appName = null;
        String appVersion = null;
        if (eventData != null && eventData.trim().length() > 0 && !"_unknown_".equals(eventData.trim())) {
            String[] values = eventData.split("(?<!\\\\),");
            if (values.length >= 1) {
                appName = values[0].trim().length() > 0 ? values[0] : null;
            }
            if (values.length >= 2) {
                appVersion = values[1].trim().length() > 0 ? values[1] : null;
            }
        }
        EndpointHandlerBuilder builder = new EndpointHandlerBuilder();
        long epochMillis = event.getEventPointData().getEventData().getEventSequence().getCreationTime().toGregorianCalendar().getTimeInMillis();
        builder.setHandlingTime(Instant.ofEpochMilli(epochMillis));
        if (appName == null) {
            String productVersion = event.getEventPointData().getEventData().getProductVersion();
            if (productVersion.startsWith("6") || productVersion.startsWith("7") || productVersion.startsWith("8")) {
                appName = "WMB";
            } else {
                appName = "IIB";
            }
            appVersion = productVersion;
        }
        builder.setApplication(new ApplicationBuilder().setName(appName).setVersion(appVersion));
        builder.setTransactionId(event.getEventPointData().getEventData().getEventCorrelation().getLocalTransactionId());
        builder.setSequenceNumber(event.getEventPointData().getEventData().getEventSequence().getCounter().intValue());
        putNonNullDataInMap("IIB_Node", event.getEventPointData().getMessageFlowData().getBroker().getName(), builder.getMetadata());
        putNonNullDataInMap("IIB_Server", event.getEventPointData().getMessageFlowData().getExecutionGroup().getName(), builder.getMetadata());
        putNonNullDataInMap("IIB_MessageFlow", event.getEventPointData().getMessageFlowData().getMessageFlow().getName(), builder.getMetadata());
        putNonNullDataInMap("IIB_MessageFlowNode", event.getEventPointData().getMessageFlowData().getNode().getNodeLabel(), builder.getMetadata());
        putNonNullDataInMap("IIB_MessageFlowNodeTerminal", event.getEventPointData().getMessageFlowData().getNode().getTerminal(), builder.getMetadata());
        putNonNullDataInMap("IIB_MessageFlowNodeType", event.getEventPointData().getMessageFlowData().getNode().getNodeType(), builder.getMetadata());

        return builder;
    }

    private void parseBitstreamAsMqMessage(Event event, byte[] decodedBitstream, MessagingTelemetryEventBuilder builder, EndpointHandlerBuilder endpointHandlerBuilder, int encoding, int ccsid)
            throws MQDataException, IOException {
        if (decodedBitstream[0] == 77 && decodedBitstream[1] == 68) {
            try (DataInputStream inputData = new DataInputStream(new ByteArrayInputStream(decodedBitstream))) {
                MQMD mqmd;
                if (encoding > 0 && ccsid > 0) {
                    mqmd = new MQMD(inputData, encoding, ccsid);
                } else {
                    mqmd = new MQMD(inputData);
                }
                putNonNullDataInMap("MQMD_CharacterSet", "" + mqmd.getCodedCharSetId(), endpointHandlerBuilder.getMetadata());
                putNonNullDataInMap("MQMD_Format", mqmd.getFormat() != null ? mqmd.getFormat().trim() : null, endpointHandlerBuilder.getMetadata());
                putNonNullDataInMap("MQMD_Encoding", "" + mqmd.getEncoding(), endpointHandlerBuilder.getMetadata());
                putNonNullDataInMap("MQMD_AccountingToken", mqmd.getAccountingToken(), endpointHandlerBuilder.getMetadata());
                putNonNullDataInMap("MQMD_Persistence", "" + mqmd.getPersistence(), endpointHandlerBuilder.getMetadata());
                putNonNullDataInMap("MQMD_Priority", "" + mqmd.getPriority(), endpointHandlerBuilder.getMetadata());
                putNonNullDataInMap("MQMD_ReplyToQueueManager", mqmd.getReplyToQMgr() != null ? mqmd.getReplyToQMgr().trim() : null, endpointHandlerBuilder.getMetadata());
                putNonNullDataInMap("MQMD_ReplyToQueue", mqmd.getReplyToQ() != null ? mqmd.getReplyToQ().trim() : null, endpointHandlerBuilder.getMetadata());
                putNonNullDataInMap("MQMD_BackoutCount", "" + mqmd.getBackoutCount(), endpointHandlerBuilder.getMetadata());
                if (mqmd.getFormat().equals(MQConstants.MQFMT_RF_HEADER_2)) {
                    new MQRFH2(inputData, mqmd.getEncoding(), mqmd.getCodedCharSetId());
                    // TODO Do something with RFH2 header?
                }

                byte[] remaining = new byte[inputData.available()];
                inputData.readFully(remaining);

                builder.setPayload(Charsets.convert(remaining, mqmd.getCodedCharSetId()));
                if (builder.getId() == null) {
                    // Event can be set earlier in case of ComIbmMQOutputNode, in that case we have to skip the set because it may fail.
                    builder.setId(byteArrayToString(mqmd.getMsgId()));
                }
                if (builder.getCorrelationId() == null) {
                    // Event can be set earlier in case of ComIbmMQOutputNode, in that case we have to skip the set because it may fail. s
                    if (mqmd.getCorrelId() != null && mqmd.getCorrelId().length > 0) {
                        builder.setCorrelationId(byteArrayToString(mqmd.getCorrelId()));
                    }
                }
                if (CMQC.MQEI_UNLIMITED != mqmd.getExpiry()) {
                    try {
                        long expiryTime = this.dateFormat.parse(mqmd.getPutDate() + mqmd.getPutTime()).getTime() + (mqmd.getExpiry() * 100);
                        builder.setExpiry(Instant.ofEpochMilli(expiryTime));
                    } catch (ParseException e) {
                        // Unable to parse put date/time. Calculate expiry based on event time.
                        long expiryTime = event.getEventPointData().getEventData().getEventSequence().getCreationTime().toGregorianCalendar().getTimeInMillis() + (mqmd.getExpiry() * 100);
                        builder.setExpiry(Instant.ofEpochMilli(expiryTime));
                    }
                }
                int ibmMsgType = mqmd.getMsgType();
                if (ibmMsgType == CMQC.MQMT_REQUEST) {
                    builder.setMessagingEventType(MessagingEventType.REQUEST);
                } else if (ibmMsgType == CMQC.MQMT_REPLY) {
                    builder.setMessagingEventType(MessagingEventType.RESPONSE);
                } else if (ibmMsgType == CMQC.MQMT_DATAGRAM) {
                    builder.setMessagingEventType(MessagingEventType.FIRE_FORGET);
                }
            }
        }
    }

    private void parseBitstreamAsHttpMessage(byte[] decoded, HttpTelemetryEventBuilder builder, EndpointBuilder endpointBuilder, EndpointHandlerBuilder endpointHandlerBuilder, int encoding, int ccsid) throws UnsupportedEncodingException {
        String codepage = null;
        if (ccsid != -1) {
            codepage = CCSID.getCodepage(ccsid);
        }
        try (BufferedReader reader = new BufferedReader(ccsid == -1 ? new InputStreamReader(new ByteArrayInputStream(decoded)) : new InputStreamReader(new ByteArrayInputStream(decoded), codepage))) {
            String line = reader.readLine();
            boolean inHeaders = false;
            if (!HttpEventType.RESPONSE.equals(builder.getHttpEventType())) {
                if (line != null) {
                    // First line is always the method + url + protocol version;
                    String[] split = line.split(" ");
                    if (split.length >= 2) {
                        builder.setHttpEventType(HttpEventType.safeValueOf(split[0]));
                        endpointBuilder.setName(split[1]);
                    }
                }
                line = reader.readLine();
                inHeaders = true;
            }
            while (line != null) {
                if (line.trim().length() == 0 && inHeaders) {
                    inHeaders = false;
                    line = reader.readLine();
                    continue;
                }
                if (inHeaders) {
                    int ix = line.indexOf(":");
                    if (ix != -1) {
                        endpointHandlerBuilder.getMetadata().put("http_" + line.substring(0, ix).trim(), line.substring(ix + 1).trim());
                    }
                } else {
                    if (builder.getPayload() != null) {
                        builder.setPayload(builder.getPayload() + "\r\n" + line);
                    } else {
                        builder.setPayload(line);
                    }
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            if (log.isDebugLevelEnabled()) {
                log.logDebugMessage("Unable to close reader.", e);
            }
        }
    }

    private void putNonNullDataInMap(String key, String value, Map<String, Object> map) {
        if (value != null && value.trim().length() > 0) {
            map.put(key, value.trim());
        }
    }

    private void putNonNullDataInMap(String key, byte[] value, Map<String, Object> map) {
        if (value != null && value.length > 0) {
            putNonNullDataInMap(key, byteArrayToString(value), map);
        }
    }

    private String byteArrayToString(byte[] bytes) {
        this.byteArrayBuilder.setLength(0);
        boolean allZero = true;
        for (byte aByte : bytes) {
            this.byteArrayBuilder.append(String.format("%02x", aByte));
            if (aByte != 0) {
                allZero = false;
            }
        }
        return allZero ? null : this.byteArrayBuilder.toString();
    }

}
