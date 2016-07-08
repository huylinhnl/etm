package com.jecstar.etm.server.core.enhancers;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.jecstar.etm.domain.Endpoint;
import com.jecstar.etm.domain.PayloadFormat;
import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.server.core.parsers.ExpressionParser;
import com.jecstar.etm.server.core.parsers.ExpressionParserField;

public class DefaultTelemetryEventEnhancer implements TelemetryEventEnhancer {
	
	private boolean enhancePayloadFormat = true;
	
	private Map<String,List<ExpressionParser>> fieldParsers = new LinkedHashMap<>();
	
	@Override
	public void enhance(TelemetryEvent<?> event, ZonedDateTime enhanceTime) {
		enchanceId(event);
		enchancePayloadFormat(event);
		enchanceWritingHandlerTimes(event, enhanceTime);
		enhanceFields(event);
	}
	
	public void setEnhancePayloadFormat(boolean enhancePayloadFormat) {
		this.enhancePayloadFormat = enhancePayloadFormat;
	}
	
	public boolean isEnhancePayloadFormat() {
		return this.enhancePayloadFormat;
	}
	
	/**
	 * Merge the field <code>ExpressionParsers</code> of another <code>DefaultTelemetryEventEnhancer</code> to this one.
	 * 
	 * @param other The other <code>DefaultTelemetryEventEnhancer</code> to merge into this one.
	 */
	public void mergeFieldParsers(DefaultTelemetryEventEnhancer other) {
		if (other.fieldParsers.isEmpty()) {
			// Nothing to merge.
			return;
		}
		if (this.fieldParsers.isEmpty()) {
			// Current parsers is empty, just overwrite with other.
			this.fieldParsers.putAll(other.fieldParsers);
			return;
		}
		for (Entry<String, List<ExpressionParser>> entry : other.fieldParsers.entrySet()) {
			if (this.fieldParsers.get(entry.getKey()) != null) {
				// Both enhancers contain the same key. Append the "other" parsers to the current ones.
				this.fieldParsers.get(entry.getKey()).addAll(entry.getValue());
			} else {
				this.fieldParsers.put(entry.getKey(), entry.getValue());
			}
		}
	}
	
	private void enchanceId(final TelemetryEvent<?> event) {
		if (event.id == null) {
			event.id = UUID.randomUUID().toString();
		}
	}
	
	private void enchancePayloadFormat(final TelemetryEvent<?> event) {
		if (event.payloadFormat == null && this.enhancePayloadFormat) {
			event.payloadFormat = detectPayloadFormat(event.payload);
		}
	}
	
	private void enchanceWritingHandlerTimes(final TelemetryEvent<?> event, final ZonedDateTime enhanceTime) {
		if (event.endpoints.size() == 0) {
			Endpoint endpoint = new Endpoint();
			endpoint.writingEndpointHandler.handlingTime = enhanceTime;
			event.endpoints.add(endpoint);
		} else {
			for (Endpoint endpoint : event.endpoints) {
				if (endpoint.writingEndpointHandler.handlingTime == null) {
					ZonedDateTime earliestReadTime = endpoint.getEarliestReadTime();
					if (earliestReadTime != null) {
						endpoint.writingEndpointHandler.handlingTime = earliestReadTime;
					} else {
						endpoint.writingEndpointHandler.handlingTime = enhanceTime;
					}
				}				
			}
		}
	}
	
	/**
	 * Super simple payload format detector. This isn't an enhanced detection
	 * algorithm because we won't be losing to much performance here. The end
	 * user should be able to tell the system which format it is anyway.
	 * 
	 * @param payload The payload.
	 * @return The detected <code>PayloadFormat</code>.
	 */
	private PayloadFormat detectPayloadFormat(String payload) {
		if (payload == null) {
			return null;
		}
		String trimmed = payload.toLowerCase().trim();
		if (trimmed.indexOf("<soap:envelope ") != -1) {
			return PayloadFormat.SOAP;
		} else if (trimmed.indexOf("<!doctype html") != -1) {
			return PayloadFormat.HTML;
		} else if (trimmed.startsWith("<?xml ")) {
			return PayloadFormat.XML;
		} else if (trimmed.startsWith("{") && payload.endsWith("}")) {
			return PayloadFormat.JSON;
		} else if (trimmed.startsWith("select")
				   || trimmed.startsWith("insert")
				   || trimmed.startsWith("update")
				   || trimmed.startsWith("delete")
				   || trimmed.startsWith("drop")
				   || trimmed.startsWith("create")) {
			return PayloadFormat.SQL;
		}
		return null;
	}
	
	
	private void enhanceFields(TelemetryEvent<?> event) {
		if (this.fieldParsers.size() == 0) {
			return;
		}
		for (Entry<String, List<ExpressionParser>> entry : this.fieldParsers.entrySet()) {
			setWhenCurrentValueEmpty(event, entry.getKey(), entry.getValue());
		}
			
	}

	private void setWhenCurrentValueEmpty(TelemetryEvent<?> event, String key, List<ExpressionParser> parsers) {
		if (parsers == null || parsers.isEmpty()) {
			return;
		}
		if (key == null) {
			return;
		}
		if (ExpressionParserField.NAME.getJsonTag().equals(key) && event.name == null ) {
			event.name = parseValue(parsers, event.payload);
		} else if (key.startsWith(ExpressionParserField.CORRELATION_DATA.getJsonTag())) {
			putInMapWhenCurrentValueEmpty(event, key, parsers, ExpressionParserField.CORRELATION_DATA, event.correlationData);
		} else if (key.startsWith(ExpressionParserField.EXTRACTED_DATA.getJsonTag())) {
			putInMapWhenCurrentValueEmpty(event, key, parsers, ExpressionParserField.EXTRACTED_DATA, event.extractedData);
		}
	}
	
	private void putInMapWhenCurrentValueEmpty(TelemetryEvent<?> event, String key, List<ExpressionParser> parsers, ExpressionParserField field, Map<String, Object> container) {
		String dataKey = field.getCollectionKeyName(key);
		if (!container.containsKey(dataKey)) {
			String value = parseValue(parsers, event.payload);
			if (value != null) {
				container.put(dataKey, value);
			}
		}		
	}
	
	private String parseValue(List<ExpressionParser> expressionParsers, String payload) {
		if (payload == null || expressionParsers == null) {
			return null;
		}
		for (ExpressionParser expressionParser : expressionParsers) {
			String value = parseValue(expressionParser, payload);
			if (value != null) {
				return value;
			}
		}
		return null;
    }
	
	private String parseValue(ExpressionParser expressionParser, String payload) {
		if (expressionParser == null || payload == null) {
			return null;
		}
		String value = expressionParser.evaluate(payload);
		if (value != null && value.trim().length() > 0) {
			return value;
		}
		return null;
	}

}
