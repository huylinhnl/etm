package com.jecstar.etm.processor.rest;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import com.jecstar.etm.core.TelemetryEventDirection;
import com.jecstar.etm.core.TelemetryEventType;
import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.jee.configurator.core.ProcessorConfiguration;
import com.jecstar.etm.processor.TelemetryEvent;
import com.jecstar.etm.processor.processor.TelemetryEventProcessor;

@Path("/event")
public class RestTelemetryEventProcessor {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(RestTelemetryEventProcessor.class);

	@Inject
	@ProcessorConfiguration
	private TelemetryEventProcessor telemetryEventProcessor;

	private final TelemetryEvent telemetryEvent = new TelemetryEvent();

	private final JsonFactory jsonFactory = new JsonFactory();

	@POST
	@Path("/add")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String addEvent(InputStream data) {
		try {
			this.telemetryEvent.initialize();
			JsonParser parser = this.jsonFactory.createJsonParser(data);
			while (parser.nextToken() != JsonToken.END_OBJECT) {
				String name = parser.getCurrentName();
				if ("application".equals(name)) {
					parser.nextToken();
					this.telemetryEvent.application = parser.getText();
				} else if ("content".equals(name)) {
					parser.nextToken();
					this.telemetryEvent.content = parser.getText();
				} else if ("creationTime".equals(name)) {
					parser.nextToken();
					this.telemetryEvent.creationTime.setTime(parser.getLongValue());
				} else if ("direction".equals(name)) {
					parser.nextToken();
					this.telemetryEvent.direction = determineDirection(parser.getText());
				} else if ("endpoint".equals(name)) {
					parser.nextToken();
					this.telemetryEvent.endpoint = parser.getText();
				} else if ("expiryTime".equals(name)) {
					parser.nextToken();
					this.telemetryEvent.expiryTime.setTime(parser.getLongValue());
				} else if ("name".equals(name)) {
					parser.nextToken();
					this.telemetryEvent.name = parser.getText();
				} else if ("sourceCorrelationId".equals(name)) {
					parser.nextToken();
					this.telemetryEvent.sourceCorrelationId = parser.getText();
				} else if ("sourceId".equals(name)) {
					parser.nextToken();
					this.telemetryEvent.sourceId = parser.getText();
				} else if ("transactionName".equals(name)) {
					parser.nextToken();
					this.telemetryEvent.transactionName = parser.getText();
				} else if ("type".equals(name)) {
					parser.nextToken();
					this.telemetryEvent.type = determineEventType(parser.getText());
				}
			}
			this.telemetryEventProcessor.processTelemetryEvent(this.telemetryEvent);
			return "{ \"status\": \"success\" }";
		} catch (IOException e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Not processing rest message.", e);
			}
			throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	private TelemetryEventDirection determineDirection(String direction) {
		try {
			return TelemetryEventDirection.valueOf(direction);
		} catch (IllegalArgumentException e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Unable to determine TelemetryEventDirection for '" +direction + "'.", e);
			}
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

	private TelemetryEventType determineEventType(String eventType) {
		try {
			return TelemetryEventType.valueOf(eventType);
		} catch (IllegalArgumentException e) {
			if (log.isErrorLevelEnabled()) {
				log.logErrorMessage("Unable to determine TelemetryEventType for '" + eventType + "'.", e);
			}
			throw new WebApplicationException(Response.Status.BAD_REQUEST);
		}
	}

}