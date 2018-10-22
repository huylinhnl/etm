package com.jecstar.etm.server.core.persisting.internal;

import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.domain.PayloadFormat;
import com.jecstar.etm.domain.builder.BusinessTelemetryEventBuilder;
import com.jecstar.etm.domain.builder.EndpointBuilder;
import com.jecstar.etm.domain.writer.json.JsonWriter;

public class BusinessEventLogger {

    private static final String BUSINESS_EVENT_ETM_STARTED = "{\"component\": \"etm\", \"node\": {0}, \"action\": \"started\"}";
    private static final String BUSINESS_EVENT_ETM_STOPPED = "{\"component\": \"etm\", \"node\": {0}, \"action\": \"stopped\"}";
    private static final String BUSINESS_EVENT_IBM_MQ_PROCESSOR_EMERGENCY_SHUTDOWN = "{\"component\": \"ibm mq processor\", \"node\": {0}, \"action\": \"emergency shutdown\", \"reason\": {1}}";
    private static final String BUSINESS_EVENT_REMOVED_INDEX = "{\"component\": \"index cleaner\", \"node\": {0}, \"action\": \"removed index\", \"index\": {1}}";
    private static final String BUSINESS_EVENT_LICENSE_EXPIRED = "{\"component\": \"etm\", \"action\": \"license expired\"}";
    private static final String BUSINESS_EVENT_LICENSE_COUNT_EXCEEDED = "{\"component\": \"etm\", \"action\": \"license count exceeded\"}";
    private static final String BUSINESS_EVENT_LICENSE_SIZE_EXCEEDED = "{\"component\": \"etm\", \"action\": \"license size exceeded\"}";
    private static final String BUSINESS_EVENT_SNMP_ENGINE_ID_ASSIGNMENT = "{\"component\": \"signaler\", \"node\": {0}, \"action\": \"SNMP engine ID assignment\", \"engineId\" : {1}}";
    private static final String BUSINESS_EVENT_SIGNAL_THRESHOLD_EXCEEDED = "{\"component\": \"signaler\", \"action\": \"signal threshold exceeded\", \"details\" : {0}}";
    private static final String BUSINESS_EVENT_SIGNAL_THRESHOLD_NO_LONGER_EXCEEDED = "{\"component\": \"signaler\", \"action\": \"signal threshold no longer exceeded\", \"details\" : {0}}";


    private static final JsonWriter jsonWriter = new JsonWriter();

    private static InternalBulkProcessorWrapper internalBulkProcessorWrapper;
    private static EndpointBuilder etmEndpoint;

    public static void initialize(InternalBulkProcessorWrapper bulkProcessorWrapper, EndpointBuilder etmEndpoint) {
        BusinessEventLogger.internalBulkProcessorWrapper = bulkProcessorWrapper;
        BusinessEventLogger.etmEndpoint = etmEndpoint;
    }

    public static void logEtmStartup() {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_ETM_STARTED
                        .replace("{0}", jsonWriter.escapeToJson(etmEndpoint.getName(), true))
                )
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("Enterprise Telemetry Monitor node started")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }

    public static void logEtmShutdown() {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_ETM_STOPPED
                        .replace("{0}", jsonWriter.escapeToJson(etmEndpoint.getName(), true))
                )
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("Enterprise Telemetry Monitor node stopped")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }

    public static void logMqProcessorEmergencyShutdown(Error e) {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_IBM_MQ_PROCESSOR_EMERGENCY_SHUTDOWN
                        .replace("{0}", jsonWriter.escapeToJson(etmEndpoint.getName(), true))
                        .replace("{1}", jsonWriter.escapeToJson(e.getMessage(), true))
                )
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("IBM MQ processor emergency shutdown")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }

    public static void logIndexRemoval(String indexName) {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_REMOVED_INDEX
                        .replace("{0}", jsonWriter.escapeToJson(etmEndpoint.getName(), true))
                        .replace("{1}", jsonWriter.escapeToJson(indexName, true))
                )
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("Index removed")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }

    public static void logLicenseExpired() {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_LICENSE_EXPIRED)
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("License expired")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }

    public static void logLicenseCountExceeded() {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_LICENSE_COUNT_EXCEEDED)
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("License count exceeded")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }

    public static void logLicenseSizeExceeded() {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_LICENSE_SIZE_EXCEEDED)
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("License size exceeded")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }

    public static void logSnmpEngineIdAssignment(String engineId) {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_SNMP_ENGINE_ID_ASSIGNMENT
                        .replace("{0}", jsonWriter.escapeToJson(etmEndpoint.getName(), true))
                        .replace("{1}", jsonWriter.escapeToJson(engineId, true))
                )
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("SNMP engine id assigned")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }

    public static void logSignalThresholdExceeded(String jsonDetailObject) {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_SIGNAL_THRESHOLD_EXCEEDED
                        .replace("{0}", jsonDetailObject)
                )
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("Signal threshold exceeded")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }

    public static void logSignalThresholdNoLongerExceeded(String jsonDetailObject) {
        BusinessTelemetryEvent businessEvent = new BusinessTelemetryEventBuilder()
                .setPayload(BUSINESS_EVENT_SIGNAL_THRESHOLD_NO_LONGER_EXCEEDED
                        .replace("{0}", jsonDetailObject)
                )
                .setPayloadFormat(PayloadFormat.JSON)
                .setName("Signal threshold no longer exceeded")
                .addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
                .build();
        BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
    }


}
