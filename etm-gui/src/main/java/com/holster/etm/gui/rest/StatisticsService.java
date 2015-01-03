package com.holster.etm.gui.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import com.holster.etm.core.logging.LogFactory;
import com.holster.etm.core.logging.LogWrapper;
import com.holster.etm.gui.rest.repository.Average;
import com.holster.etm.gui.rest.repository.ExpiredMessage;
import com.holster.etm.gui.rest.repository.StatisticsRepository;

@Path("/statistics")
public class StatisticsService {
	
	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(StatisticsService.class);

	
	@Inject
	private StatisticsRepository statisticsRepository;
	
	private final JsonFactory jsonFactory = new JsonFactory();

	@GET
	@Path("/transactions/performance/{starttime}/{endtime}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getTransactionsPerformanceFromTimePeriod(@PathParam("starttime") Long startTime, @PathParam("endtime") Long endTime, @QueryParam("max") int max) {
		if (max == 0) {
			max = 5;
		}
		if (startTime > endTime) {
			return "[]";
		}
		Map<String, Map<Long, Average>> statistics = this.statisticsRepository.getTransactionPerformanceStatistics(startTime, endTime, max, determineTimeUnit(startTime, endTime));
		StringWriter writer = new StringWriter();
		writeMessagesAverageStatistics(writer, statistics);
		return writer.toString();
	}
	
	@GET
	@Path("/messages/performance/{starttime}/{endtime}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getMessagesPerformanceFromTimePeriod(@PathParam("starttime") Long startTime, @PathParam("endtime") Long endTime, @QueryParam("max") int max) {
		if (max == 0) {
			max = 5;
		}
		if (startTime > endTime) {
			return "[]";
		}
		Map<String, Map<Long, Average>> statistics = this.statisticsRepository.getMessagesPerformanceStatistics(startTime, endTime, max, determineTimeUnit(startTime, endTime));
		StringWriter writer = new StringWriter();
		writeMessagesAverageStatistics(writer, statistics);
		return writer.toString();
	}
	
	@GET
	@Path("/messages/epxiration/{starttime}/{endtime}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getMessagesExpirationFromTimePeriod(@PathParam("starttime") Long startTime, @PathParam("endtime") Long endTime, @QueryParam("max") int max) {
		if (max == 0) {
			max = 5;
		}
		if (startTime > endTime) {
			return "[]";
		}
		List<ExpiredMessage> statistics = this.statisticsRepository.getMessagesExpirationStatistics(startTime, endTime, max);
		StringWriter writer = new StringWriter();
		writeMessagesExpirationStatistics(writer, statistics);
		return writer.toString();
	}

	@GET
	@Path("/applications/count/{starttime}/{endtime}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getApplicationsCountFromTimePeriod(@PathParam("starttime") Long startTime, @PathParam("endtime") Long endTime, @QueryParam("max") int max) {
		if (max == 0) {
			max = 10;
		}
		Map<String, Map<String, Long>> statistics = this.statisticsRepository.getApplicationCountStatistics(startTime, endTime, max);
		StringWriter writer = new StringWriter();
        List<String> applictions = new ArrayList<String>(statistics.keySet());
        List<String> serieNames = new ArrayList<String>(4);
        serieNames.add("Incoming request messages");
        serieNames.add("Outgoing request messages");
        serieNames.add("Incoming datagram messages");
        serieNames.add("Outgoing datagram messages");
        List<Long> incomingRequests = new ArrayList<Long>(statistics.size());
        List<Long> outgoingRequests = new ArrayList<Long>(statistics.size());
        List<Long> incomingDatagrams = new ArrayList<Long>(statistics.size());
        List<Long> outgoingDatagrams = new ArrayList<Long>(statistics.size());
        Collections.sort(applictions);
        for (String application : applictions) {
        	Map<String, Long> appStats = statistics.get(application);
        	incomingRequests.add(appStats.get("incomingMessageRequest"));
        	outgoingRequests.add(appStats.get("outgoingMessageRequest"));
        	incomingDatagrams.add(appStats.get("incomingDatagramRequest"));
        	outgoingDatagrams.add(appStats.get("outgoingDatagramRequest"));
        }
        List<List<Long>> values = new ArrayList<List<Long>>();
        values.add(incomingRequests);
        values.add(outgoingRequests);
        values.add(incomingDatagrams);
        values.add(outgoingDatagrams);
		writeApplicationCountStatistics(writer, applictions, serieNames, values);
		return writer.toString();
	}
	
	private void writeApplicationCountStatistics(Writer writer, List<String> categories, List<String> serieNames, List<List<Long>>values) {
		try {
	        JsonGenerator generator = this.jsonFactory.createJsonGenerator(writer);
	        generator.writeStartObject();
	        generator.writeArrayFieldStart("categories");
	        for (String category : categories) {
	        	generator.writeString(category);
	        }
	        generator.writeEndArray();
	        generator.writeArrayFieldStart("series");
	        for (int i=0; i < serieNames.size(); i++) {
	        	generator.writeStartObject();
	        	generator.writeStringField("id", serieNames.get(i));
	        	generator.writeStringField("name", serieNames.get(i));
	        	generator.writeArrayFieldStart("data");
	        	List<Long> serieValues = values.get(i);
	        	for (Long value : serieValues) {
	        		generator.writeNumber(value);
	        	}
	        	generator.writeEndArray();
	        	generator.writeEndObject();
	        }
	        generator.writeEndArray();
	        generator.writeEndObject();
	        generator.close();
        } catch (IOException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Unable to generate count statistics.", e);
        	}
        }		
	}
	
	private void writeMessagesAverageStatistics(Writer writer, Map<String, Map<Long, Average>> statistics) {
		try {
	        JsonGenerator generator = this.jsonFactory.createJsonGenerator(writer);
	        generator.writeStartArray();
	        for (String name : statistics.keySet()) {
	        	generator.writeStartObject();
	        	generator.writeStringField("name", name);
	        	generator.writeStringField("id", name);
	        	generator.writeArrayFieldStart("data");
	        	Map<Long, Average> values = statistics.get(name);
	        	SortedSet<Long> keys = new TreeSet<Long>(values.keySet());
	        	for (long time: keys) {
	        		generator.writeStartObject();
	        		generator.writeStringField("id", name + "_" + time);
	        		generator.writeNumberField("x", time);
	        		generator.writeNumberField("y", values.get(time).getAverage());
	        		generator.writeEndObject();
	        	}
	        	generator.writeEndArray();
	        	generator.writeEndObject();
	        }
	        generator.writeEndArray();
	        generator.close();
        } catch (IOException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Unable to generate performance statistics.", e);
        	}
        }
	}

    private void writeMessagesExpirationStatistics(StringWriter writer, List<ExpiredMessage> statistics) {
		try {
	        JsonGenerator generator = this.jsonFactory.createJsonGenerator(writer);
	        generator.writeStartArray();
	        for (ExpiredMessage expiredMessage: statistics) {
	        	generator.writeStartObject();
	        	generator.writeStringField("name", expiredMessage.getName());
	        	generator.writeStringField("application", expiredMessage.getApplication());
	        	generator.writeNumberField("startTime", expiredMessage.getStartTime().getTime());
	        	generator.writeNumberField("expirationTime", expiredMessage.getExpirationTime().getTime());
	        	generator.writeEndObject();
	        }
	        generator.writeEndArray();
	        generator.close();
        } catch (IOException e) {
        	if (log.isErrorLevelEnabled()) {
        		log.logErrorMessage("Unable to generate performance statistics.", e);
        	}
        }    }

	
	private TimeUnit determineTimeUnit(long startTime, long endTime) {
		long duration = endTime - startTime;
		if (duration >= 864000000l) { // Ten days
			return TimeUnit.DAYS;
		} else if (duration >= 36000000l) { // Ten hours
			return TimeUnit.HOURS;
		} else if (duration > 600000l) { // Ten minutes
			return TimeUnit.MINUTES;
		} else if(duration > 10000l) { // Ten seconds
			return TimeUnit.SECONDS;
		} else {
			return TimeUnit.MILLISECONDS;
		}
	}
}
