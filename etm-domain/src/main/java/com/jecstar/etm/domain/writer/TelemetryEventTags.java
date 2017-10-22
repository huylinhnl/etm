package com.jecstar.etm.domain.writer;

public interface TelemetryEventTags {

    public static final String EVENT_TYPE_BUSINESS = "business";
    public static final String EVENT_TYPE_HTTP = "http";
    public static final String EVENT_TYPE_LOG = "log";
    public static final String EVENT_TYPE_MESSAGING = "messaging";
    public static final String EVENT_TYPE_SQL = "sql";

	// TelemetryEvent attributes.
	String getTimestampTag();
	String getTypeTag();
	String getIdTag();
	String getCorrelationIdTag();
	String getCorrelationDataTag();
	String getCorrelationsTag();
	String getExtractedDataTag();
	String getEventHashesTag();
	String getNameTag();
	String getMetadataTag();
	String getPayloadTag();
	String getPayloadFormatTag();
	String getPayloadLengthTag();
	String getTransportTag();
	String getReadingEndpointHandlerTag();
	String getReadingEndpointHandlersTag();
	String getWritingEndpointHandlerTag();
	
	// Endpoint attributes;
	String getEndpointsTag();
	String getEndpointNameTag();

	// EndpointHandler attributes
	String getEndpointHandlerHandlingTimeTag();
	String getEndpointHandlerLatencyTag();
	String getEndpointHandlerResponseTimeTag();
	String getEndpointHandlerTransactionIdTag();
	String getEndpointHandlerForcedTag();
	String getEndpointHandlerApplicationTag();
	// Application attributes
	String getApplicationNameTag();
	String getApplicationHostAddressTag();
	String getApplicationHostNameTag();
	String getApplicationInstanceTag();
	String getApplicationVersionTag();
	String getApplicationPrincipalTag();
	
	// Location attributes.
	String getEndpointHandlerLocationTag();
	String getLatitudeTag();
	String getLongitudeTag();
	
	// Messaging event attributes
	String getExpiryTag();
	String getMessagingEventTypeTag();
	
	// Log event attributes
	String getLogLevelTag();
	String getStackTraceTag();
	
	// Http event attributes
	String getHttpEventTypeTag();
	
	// Sql event attributes
	String getSqlEventTypeTag();
}
