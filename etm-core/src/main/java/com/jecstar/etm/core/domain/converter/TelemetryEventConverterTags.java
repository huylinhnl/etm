package com.jecstar.etm.core.domain.converter;

public interface TelemetryEventConverterTags {

	// Map attributes
	String getMapKeyTag();
	String getMapValueTag();
	
	// TelemetryEvent attributes.
	String getIdTag();
	String getCorrelationIdTag();
	String getCorrelationDataTag();
	String getEndpointTag();
	String getExpiryTag();
	String getExtractedDataTag();
	String getNameTag();
	String getMetadataTag();
	String getPackagingTag();
	String getPayloadTag();
	String getPayloadFormatTag();
	String getResponseTimeTag();
	String getResponseHandlingTimeTag();
	String getTransactionIdTag();
	String getTransportTag();
	String getReadingEndpointHandlersTag();
	String getWritingEndpointHandlerTag();
	
	// EndpointHandler attributes
	String getEndpointHandlerHandlingTimeTag();
	String getEndpointHandlerApplicationTag();
	
	// Application attributes
	String getApplicationNameTag();
	String getApplicationHostAddressTag();
	String getApplicationInstanceTag();
	String getApplicationVersionTag();
	String getApplicationPrincipalTag();
	
	// Location attributes.
	String getEndpointHandlerLocationTag();
	String getLatitudeTag();
	String getLongitudeTag();
}
