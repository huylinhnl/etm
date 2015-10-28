package com.jecstar.etm.processor.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.jecstar.etm.processor.processor.TelemetryCommandProcessor;

public class RestTelemetryEventProcessorApplication extends Application {

	public RestTelemetryEventProcessorApplication(TelemetryCommandProcessor processor) {
		RestTelemetryEventProcessor.setProcessor(processor);
	}

	@Override
    public Set<Class<?>> getClasses()
    {
       HashSet<Class<?>> classes = new HashSet<Class<?>>();
       classes.add(RestTelemetryEventProcessor.class);
       return classes;
    }
}
