package com.jecstar.etm.server.core.parsers;

import com.jecstar.etm.domain.writers.json.TelemetryEventTagsJsonImpl;

public enum ExpressionParserField {
	
	ID(new TelemetryEventTagsJsonImpl().getIdTag()),
	CORRELATION_ID(new TelemetryEventTagsJsonImpl().getCorrelationIdTag()),
	NAME(new TelemetryEventTagsJsonImpl().getNameTag()), 
	WRITER_TRANSACTION_ID(new TelemetryEventTagsJsonImpl().getEndpointsTag() + "." + new TelemetryEventTagsJsonImpl().getWritingEndpointHandlerTag() + "." + new TelemetryEventTagsJsonImpl().getEndpointHandlerTransactionIdTag()),
	READER_TRANSACTION_ID(new TelemetryEventTagsJsonImpl().getEndpointsTag() + "." + new TelemetryEventTagsJsonImpl().getReadingEndpointHandlerTag() + "." + new TelemetryEventTagsJsonImpl().getEndpointHandlerTransactionIdTag()),
	CORRELATION_DATA(new TelemetryEventTagsJsonImpl().getCorrelationDataTag() + "."), 
	EXTRACTED_DATA(new TelemetryEventTagsJsonImpl().getExtractedDataTag() + ".");
	
	private final String jsonTag;

	private ExpressionParserField(String jsonTag) {
		this.jsonTag = jsonTag;
	}
	
	public String getJsonTag() {
		return this.jsonTag;
	}
	
	public String getCollectionKeyName(String fullKey) {
		int ix = fullKey.indexOf(getJsonTag());
		if (ix == -1) {
			return null;
		}
		return fullKey.substring(ix + getJsonTag().length());
	}
}
