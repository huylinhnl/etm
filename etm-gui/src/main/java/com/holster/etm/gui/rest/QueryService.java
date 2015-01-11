package com.holster.etm.gui.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.impl.DefaultPrettyPrinter;

import com.holster.etm.gui.rest.repository.QueryRepository;
import com.holster.etm.jee.configurator.core.GuiConfiguration;

@Path("/query")
public class QueryService {
	
	@GuiConfiguration
	@Inject
	private SolrServer solrServer;
	
	@Inject
	private QueryRepository queryRepository;

	private final JsonFactory jsonFactory = new JsonFactory();

	
	@GET
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String performUnformattedQuery(@QueryParam("queryString") String queryString ,@QueryParam("start") int start, @QueryParam("rows") int rows) {
		if (rows <= 0) {
			rows = 25;
		} else if (rows > 1000) {
			rows = 1000;
		}
		SolrQuery query = new SolrQuery(queryString);
		query.setStart(start);
		query.setRows(rows);
		try {
			long startTime = System.nanoTime();
	        QueryResponse queryResponse = this.solrServer.query(query);
	        SolrDocumentList results = queryResponse.getResults();
	        StringWriter writer = new StringWriter();
	        JsonGenerator generator = this.jsonFactory.createJsonGenerator(writer);
	        generator.writeStartObject();
	        generator.writeNumberField("numFound", results.getNumFound());
	        generator.writeNumberField("numReturned", results.size());
	        generator.writeNumberField("start", results.getStart());
	        generator.writeNumberField("end", results.getStart() + results.size() - 1);
	        generator.writeArrayFieldStart("events");
	        this.queryRepository.addEvents(results, generator);
	        generator.writeEndArray();
	        generator.writeNumberField("queryTime", TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS));
	        generator.writeEndObject();
	        generator.close();
	        return writer.toString();
        } catch (SolrServerException e) {
	        // TODO Error handling
        	e.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        	// TODO Error handling
        }
		return "{}";
	}
	
	@GET
	@Path("/event/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getEventById(@PathParam("id") String id) {
		UUID eventId = null;
		try {
			eventId = UUID.fromString(id);
	        StringWriter writer = new StringWriter();
	        JsonGenerator generator = this.jsonFactory.createJsonGenerator(writer);
	        generator.writeStartObject();
	        this.queryRepository.addEvent(eventId, generator);
	        generator.writeEndObject();
	        generator.close();
	        return writer.toString();
		} catch (Exception e) {
			e.printStackTrace();
			// TODO error handling
		}
		return "{}";
	}

	
	@GET
	@Path("/event/{id}/overview")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String getEventOverviewById(@PathParam("id") String id) {
		UUID eventId = null;
		try {
			eventId = UUID.fromString(id);
	        StringWriter writer = new StringWriter();
	        JsonGenerator generator = this.jsonFactory.createJsonGenerator(writer);
	        generator.writeStartObject();
	        this.queryRepository.addEventOverview(eventId, generator);
	        generator.writeEndObject();
	        generator.close();
	        return writer.toString();
		} catch (Exception e) {
			e.printStackTrace();
			// TODO error handling
		}
		return "{}";

	}

}