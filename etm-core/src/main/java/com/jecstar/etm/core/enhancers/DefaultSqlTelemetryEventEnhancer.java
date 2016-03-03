package com.jecstar.etm.core.enhancers;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.jecstar.etm.core.domain.Endpoint;
import com.jecstar.etm.core.domain.PayloadFormat;
import com.jecstar.etm.core.domain.SqlTelemetryEvent;

public class DefaultSqlTelemetryEventEnhancer implements SqlTelemetryEventEnhancer {

	@Override
	public String getName() {
		return "Default SQL Enhancer";
	}
	
	@Override
	public void enhance(final SqlTelemetryEvent event, final ZonedDateTime enhanceTime) {
		if (event.id == null) {
			event.id = UUID.randomUUID().toString();
		}
		if (event.payloadFormat == null) {
			event.payloadFormat = PayloadFormat.SQL;
		}
		if (event.endpoints.size() == 0) {
			Endpoint endpoint = new Endpoint();
			endpoint.writingEndpointHandler.handlingTime = enhanceTime;
			event.endpoints.add(endpoint);
		} else {
			event.endpoints.forEach(c -> {
				if (c.writingEndpointHandler.handlingTime == null) {
					c.writingEndpointHandler.handlingTime = enhanceTime;
				}
			});
		}
	}

}
