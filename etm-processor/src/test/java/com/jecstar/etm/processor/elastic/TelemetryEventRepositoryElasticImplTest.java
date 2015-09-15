package com.jecstar.etm.processor.elastic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.core.domain.EndpointHandler;
import com.jecstar.etm.core.domain.PayloadFormat;
import com.jecstar.etm.core.domain.TelemetryEvent;

/**
 * Test class form the <code>TelemetryEventRepositoryElasticImpl</code> class.
 * 
 * @author mark
 */
public class TelemetryEventRepositoryElasticImplTest {

	private static Node node;
	private Client client;
	private final String nodeName = "etm-test";

	@BeforeClass
	public static void beforeClass() {
		node = new NodeBuilder().settings(ImmutableSettings.settingsBuilder()
				.put("http.enabled", false)
				.put("path.conf", "src/test/resources/conf"))
				.local(true).node();
	}
	
	@AfterClass
	public static void afterClass() {
		if (node != null) {
			File dataDirectory = new File(node.settings().get("path.data") == null ? "data" : node.settings().get("path.data"));
			node.close();
			deleteDirectory(dataDirectory);
		}
	}
	
	private static boolean deleteDirectory(File path) {
	    if( path.exists() ) {
	      File[] files = path.listFiles();
	      for(int i=0; i<files.length; i++) {
	         if(files[i].isDirectory()) {
	           deleteDirectory(files[i]);
	         }
	         else {
	           files[i].delete();
	         }
	      }
	    }
	    return( path.delete() );
	  }
	
	@Before
	public void before() {
		this.client = node.client();
	}
	
	@After
	public void after() throws ExecutionException {
		this.client.admin().indices().prepareDelete("_all").get();
	}
	
	/**
	 * Test persistence of a single event with a writing and one reading application.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testPersistOneWriterOneReaderSingleEvent() {
		final String id = "1";
		TelemetryEvent event = new TelemetryEvent();
		event.id = id;
		event.payloadFormat = PayloadFormat.TEXT;
		event.payload = "Testcase testPersistOneWriterOneReaderSingleEvent.";
		event.writingEndpointHandler.application.name = "TestCase";
		event.writingEndpointHandler.handlingTime = ZonedDateTime.now();
		EndpointHandler endpointHandler = new EndpointHandler();
		endpointHandler.handlingTime = ZonedDateTime.now();
		endpointHandler.application.name = "TestCase";
		event.readingEndpointHandlers.add(endpointHandler);
		// TODO add all possible attributes of an event to the test.
		try (TelemetryEventRepositoryElasticImpl repo = new TelemetryEventRepositoryElasticImpl(createSingleCommitConfiguration(), this.client)) {
			repo.persistTelemetryEvent(event);
			
			// Validate all elements.
			GetResponse getResponse = this.client.prepareGet(repo.getElasticIndexName(event), repo.getElasticType(event), id).get();
			Map<String, Object> source = getResponse.getSourceAsMap();
			assertEquals(event.id, source.get("id"));
			assertEquals(event.payload, source.get("payload"));
			assertEquals(event.payloadFormat.name(), source.get("payload_format"));
			List<Map<String, Object>> readingEndpointHandlers = (List<Map<String, Object>>) source.get("reading_endpoint_handlers");
			assertEquals(endpointHandler.handlingTime.toInstant().toEpochMilli(), readingEndpointHandlers.get(0).get("handling_time"));
			assertEquals(endpointHandler.application.name, ((Map<String, Object>)readingEndpointHandlers.get(0).get("application")).get("name"));
			Map<String, Object> writingEndpointHandler = (Map<String, Object>) source.get("writing_endpoint_handler");
			assertEquals(event.writingEndpointHandler.handlingTime.toInstant().toEpochMilli(), writingEndpointHandler.get("handling_time"));
			assertEquals(event.writingEndpointHandler.application.name, ((Map<String, Object>)writingEndpointHandler.get("application")).get("name"));
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPersistOneWriterTwoReadersSeparatedEvents() {
		final String id = "2";
		TelemetryEvent event = new TelemetryEvent();
		event.id = id;
		event.payloadFormat = PayloadFormat.TEXT;
		event.payload = "Testcase testPersistOneWriterTwoReadersSeparatedEvents.";
		event.writingEndpointHandler.application.name = "TestCase";
		event.writingEndpointHandler.handlingTime = ZonedDateTime.now();
		try (TelemetryEventRepositoryElasticImpl repo = new TelemetryEventRepositoryElasticImpl(createSingleCommitConfiguration(), this.client)) {
			// Add the event from the writing application.
			repo.persistTelemetryEvent(event);
			
			// Add the event from the first reading application.
			event.initialize();
			event.id = id;
			event.payloadFormat = PayloadFormat.TEXT;
			event.payload = "Testcase testPersistOneWriterTwoReadersSeparatedEvents.";
			EndpointHandler endpointHandler = new EndpointHandler();
			endpointHandler.handlingTime = ZonedDateTime.now();
			endpointHandler.application.name = "Reading app 1";
			event.readingEndpointHandlers.add(endpointHandler);
			repo.persistTelemetryEvent(event);

			// Add the event from the second reading application.
			event.initialize();
			event.id = id;
			event.payloadFormat = PayloadFormat.TEXT;
			event.payload = "Testcase testPersistOneWriterTwoReadersSeparatedEvents.";
			endpointHandler = new EndpointHandler();
			endpointHandler.handlingTime = ZonedDateTime.now().plus(10, ChronoUnit.SECONDS);
			endpointHandler.application.name = "Reading app 2";
			event.readingEndpointHandlers.add(endpointHandler);
			repo.persistTelemetryEvent(event);
			
			GetResponse getResponse = this.client.prepareGet(repo.getElasticIndexName(event), repo.getElasticType(event), id).get();
			Map<String, Object> source = getResponse.getSourceAsMap();
			List<Map<String, Object>> readingEndpointHandlers = (List<Map<String, Object>>) source.get("reading_endpoint_handlers");
			assertEquals(2, readingEndpointHandlers.size());
		}
	}
	
	@Test
	public void testPersistRequestBeforeResponseEvent() {
		// TODO testcase waarbij het request voor het response wordt geschreven. De expiry time moet geupdate worden.
		fail("To be implemented.");
	}
	
	@Test
	public void testPersistResponseBeforeRequestEvent() {
		// TODO testcase waarbij het response voor het request wordt geschreven. De expiry time moet correct berekend worden.
		fail("To be implemented.");
	}
	
	private EtmConfiguration createSingleCommitConfiguration() {
		Properties properties = new Properties();
		properties.setProperty(EtmConfiguration.ETM_PERSISTING_BULK_SIZE, "1");
		EtmConfiguration configuration = new EtmConfiguration(this.nodeName, this.client, this.getClass().getName());
		configuration.update(this.nodeName, properties);
		return configuration;
	}

}
