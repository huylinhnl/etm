package com.jecstar.etm.processor.repository.elasticsearch;

import java.util.HashMap;
import java.util.Map;

import com.jecstar.etm.processor.TelemetryEvent;
import com.jecstar.etm.processor.converter.TelemetryEventConverterTags;

/**
 * Class capable of building the update map that can be provided to the scripts.
 * 
 * @author mark
 */
public class UpdateScriptBuilder {

	UpdateScriptBuilder() {
	}

	Map<String, Object> createUpdateParameterMap(TelemetryEvent event, TelemetryEventConverterTags tags) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		addValueSetterToScript(parameters, tags.getIdTag(), event.id);
		addValueSetterToScript(parameters, tags.getApplicationTag(), event.application);
		addValueSetterToScript(parameters, tags.getContentTag(), event.content);
		addValueSetterToScript(parameters, tags.getCorrelationIdTag(), event.correlationId);
		addMapAppenderToScript(parameters, tags.getCorrelationDataTag(), event.correlationData);
		addValueSetterToScript(parameters, tags.getCreationTimeTag(), event.creationTime.getTime() == 0 ? null : event.creationTime.getTime());
		addValueSetterToScript(parameters, tags.getEndpointTag(), event.endpoint);
		addValueSetterToScript(parameters, tags.getExpiryTimeTag(), event.expiryTime.getTime() == 0 ? null : event.expiryTime.getTime());
		if (event.direction != null) {
			addValueSetterToScript(parameters, tags.getDirectionTag(), event.direction.name());
		}
		addMapAppenderToScript(parameters, tags.getMetadataTag(), event.metadata);
		addValueSetterToScript(parameters, tags.getNameTag(), event.name);
		addValueSetterToScript(parameters, tags.getTransactionIdTag(), event.transactionId);
		addValueSetterToScript(parameters, tags.getTransactionNameTag(), event.transactionName);
		if (event.type != null) {
			addValueSetterToScript(parameters, tags.getTypeTag(), event.type.name());
		}
		return parameters;
	}
	
	Map<String, Object> createRequestUpdateScriptFromResponse(TelemetryEvent event, TelemetryEventConverterTags tags) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(tags.getCreationTimeTag(), event.creationTime.getTime() == 0 ? null : event.creationTime.getTime());
		parameters.put(tags.getCorrelationIdTag(), event.correlationId);
		return parameters;
	}
	
	private void addValueSetterToScript(Map<String, Object> parameters, String tag, Object value) {
		parameters.put(tag, value);
	}
	
	private void addMapAppenderToScript(Map<String, Object> parameters, String tag, Map<String, String> map) {
		if (map == null || map.size() <= 0) {
			parameters.put(tag, null);		
			return;
		}
		Map<String, Object> jsonData = new HashMap<String, Object>();
		for (String key : map.keySet()) {
			jsonData.put(key, map.get(key));
		}
		parameters.put(tag, jsonData);		
	}
}