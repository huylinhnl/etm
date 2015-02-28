package com.jecstar.etm.processor.repository;

import java.util.concurrent.TimeUnit;

import com.jecstar.etm.processor.TelemetryEvent;

public interface TelemetryEventRepository {

	void persistTelemetryEvent(TelemetryEvent telemetryEvent, TimeUnit statisticsTimeUnit);

	void findParent(String sourceId, CorrelationBySourceIdResult result);
	
	void findEndpointConfig(String endpoint, EndpointConfigResult result, long cacheExpiryTime);
}