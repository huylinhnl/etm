package com.jecstar.etm.processor.internal.persisting;

import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.domain.PayloadFormat;
import com.jecstar.etm.domain.builders.BusinessTelemetryEventBuilder;
import com.jecstar.etm.domain.builders.EndpointBuilder;
import com.jecstar.etm.domain.writers.json.JsonWriter;

public class BusinessEventLogger {

	private static final String BUSINESS_EVENT_ETM_STARTED = "{\"component\": \"etm\", \"node\": \"{0}\", \"action\": \"started\"}";
	private static final String BUSINESS_EVENT_ETM_STOPPED = "{\"component\": \"etm\", \"node\": \"{0}\", \"action\": \"stopped\"}";
	private static final String BUSINESS_EVENT_IBM_MQ_PROCESSOR_EMERGENCY_SHUTDOWN = "{\"component\": \"ibm mq processor\", \"node\": \"{0}\", \"action\": \"emergency shutdown\", \"reason\": \"{1}\"}";
	private static final String BUSINESS_EVENT_REMOVED_INDEX = "{\"component\": \"index cleaner\", \"node\": \"{0}\", \"action\": \"removed index\", \"index\": \"{1}\"}";
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
				.replace("{0}", etmEndpoint.getName())
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
				.replace("{0}", etmEndpoint.getName())
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
				.replace("{0}", etmEndpoint.getName())
				.replace("{1}", jsonWriter.escapeToJson(e.getMessage(), false))
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
				.replace("{0}", etmEndpoint.getName())
				.replace("{1}", jsonWriter.escapeToJson(indexName, false))
			)
			.setPayloadFormat(PayloadFormat.JSON)
			.setName("Index removed")
			.addOrMergeEndpoint(etmEndpoint.setWritingTimeToNow())
			.build();
		BusinessEventLogger.internalBulkProcessorWrapper.persist(businessEvent);
	}
}
