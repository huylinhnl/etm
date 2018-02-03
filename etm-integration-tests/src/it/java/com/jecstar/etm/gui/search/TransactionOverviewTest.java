package com.jecstar.etm.gui.search;

import com.jecstar.etm.domain.*;
import com.jecstar.etm.domain.HttpTelemetryEvent.HttpEventType;
import com.jecstar.etm.domain.MessagingTelemetryEvent.MessagingEventType;
import com.jecstar.etm.domain.SqlTelemetryEvent.SqlEventType;
import com.jecstar.etm.domain.builder.*;
import com.jecstar.etm.domain.writer.TelemetryEventWriter;
import com.jecstar.etm.domain.writer.json.HttpTelemetryEventWriterJsonImpl;
import com.jecstar.etm.domain.writer.json.LogTelemetryEventWriterJsonImpl;
import com.jecstar.etm.domain.writer.json.MessagingTelemetryEventWriterJsonImpl;
import com.jecstar.etm.domain.writer.json.SqlTelemetryEventWriterJsonImpl;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Class testing the event overview.
 * 
 * @author Mark Holster
 */
public class TransactionOverviewTest extends AbstractSearchIntegrationTest {
	
	private final TelemetryEventWriter<String, HttpTelemetryEvent> httpEventWriter = new HttpTelemetryEventWriterJsonImpl(); 
	private final TelemetryEventWriter<String, LogTelemetryEvent> logEventWriter = new LogTelemetryEventWriterJsonImpl(); 
	private final TelemetryEventWriter<String, MessagingTelemetryEvent> mqEventWriter = new MessagingTelemetryEventWriterJsonImpl(); 
	private final TelemetryEventWriter<String, SqlTelemetryEvent> sqlEventWriter = new SqlTelemetryEventWriterJsonImpl(); 

	@Test
	public void testEventOverview() throws IOException {
		final String eventId = UUID.randomUUID().toString();
		final EndpointHandlerBuilder guiEndpointHandler = new EndpointHandlerBuilder()
				.setTransactionId(UUID.randomUUID().toString())
				.setApplication(new ApplicationBuilder()
						.setName("Gui application")
						.setVersion("1.0.0")
				);
		final EndpointHandlerBuilder backendEndpointHandler = new EndpointHandlerBuilder()
				.setTransactionId(UUID.randomUUID().toString())
				.setApplication(new ApplicationBuilder()
						.setName("My Backend")
						.setVersion("2.1.0_beta3")
				);
		
		// A user requests the shopping card page from our public http site.
		ZonedDateTime timestamp = ZonedDateTime.now();
		guiEndpointHandler.setHandlingTime(timestamp);
		assertTrue(sendEventToEtm("http", this.httpEventWriter.write(new HttpTelemetryEventBuilder()
				.setId(eventId)
				.setPayload("GET http://www.my-company.com/shopping-card.html")
				.setName("GetShoppingCard")
				.setPayloadFormat(PayloadFormat.HTML)
				.setHttpEventType(HttpEventType.GET)
				.setExpiry(timestamp.plusSeconds(30))
				.addMetadata("User-Agent", "Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:46.0) Gecko/20100101 Firefox/46.0")
				.addMetadata("Pragma", "no-cache")
				.addOrMergeEndpoint(new EndpointBuilder()
										.setName("/shopping-card.html")
										.addReadingEndpointHandler(guiEndpointHandler)
									)
				.build())));
		
		// Add some logging generated by our gui app.
		timestamp = timestamp.plus(15, ChronoUnit.MILLIS);
        guiEndpointHandler.setHandlingTime(timestamp);
        guiEndpointHandler.setSequenceNumber(2);
        assertTrue(sendEventToEtm("log", this.logEventWriter.write(new LogTelemetryEventBuilder()
                .setId(UUID.randomUUID().toString())
                .setPayload("Found user")
                .setPayloadFormat(PayloadFormat.TEXT)
                .setLogLevel("DEBUG")
                .addOrMergeEndpoint(new EndpointBuilder()
                        .setName("com.my-company.gui.Httphandler.handleRequest(Httphandler.java:60")
                        .setWritingEndpointHandler(guiEndpointHandler)
                )
                .build())));

        guiEndpointHandler.setSequenceNumber(1);
        assertTrue(sendEventToEtm("log", this.logEventWriter.write(new LogTelemetryEventBuilder()
				.setId(UUID.randomUUID().toString())
				.setPayload("User is requesting his/her shopping card.")
				.setPayloadFormat(PayloadFormat.TEXT)
				.setLogLevel("DEBUG")
				.addOrMergeEndpoint(new EndpointBuilder()
							.setName("com.my-company.gui.Httphandler.handleRequest(Httphandler.java:59)")
							.setWritingEndpointHandler(guiEndpointHandler)
						)
				.build())));


        timestamp = timestamp.plus(8, ChronoUnit.MILLIS);
		guiEndpointHandler.setHandlingTime(timestamp);
        guiEndpointHandler.setSequenceNumber(3);
		assertTrue(sendEventToEtm("log", this.logEventWriter.write(new LogTelemetryEventBuilder()
				.setId(UUID.randomUUID().toString())
				.setPayload("Requesting shoppping card over MQ.")
				.setPayloadFormat(PayloadFormat.TEXT)
				.setLogLevel("DEBUG")
				.addOrMergeEndpoint(new EndpointBuilder()
							.setName("com.my-company.gui.MqRequestor.requestShoppingCar(MqRequestor.java:352)")
							.setWritingEndpointHandler(guiEndpointHandler)
						)
				.build())));
		
		// Now let the gui app send an MQ request to the backend app. 
		timestamp = timestamp.plus(1, ChronoUnit.MILLIS);
		guiEndpointHandler.setHandlingTime(timestamp);
        guiEndpointHandler.setSequenceNumber(null);
		timestamp = timestamp.plus(3, ChronoUnit.MILLIS);
		backendEndpointHandler.setHandlingTime(timestamp);
		String mqMessageId = UUID.randomUUID().toString();
		assertTrue(sendEventToEtm("messaging", this.mqEventWriter.write(new MessagingTelemetryEventBuilder()
				.setId(mqMessageId)
				.setPayload("<shoppingcard_request><customer_id>543214</customer_id></shoppingcard_request>")
				.setPayloadFormat(PayloadFormat.XML)
				.setMessagingEventType(MessagingEventType.REQUEST)
				.setName("ShoppingCardRequest")
				.setExpiry(timestamp.plusSeconds(30))
				.addOrMergeEndpoint(new EndpointBuilder()
							.setName("BACKEND.QUEUE.1")
							.setWritingEndpointHandler(guiEndpointHandler)
							.addReadingEndpointHandler(backendEndpointHandler)
						)
				.build())));
		
		// Add some backend logging
		timestamp = timestamp.plus(7, ChronoUnit.MILLIS);
		backendEndpointHandler.setHandlingTime(timestamp);
		assertTrue(sendEventToEtm("log", this.logEventWriter.write(new LogTelemetryEventBuilder()
				.setId(UUID.randomUUID().toString())
				.setPayload("Received shopping card request.")
				.setPayloadFormat(PayloadFormat.TEXT)
				.setLogLevel("DEBUG")
				.addOrMergeEndpoint(new EndpointBuilder()
							.setName("com.my-company.backend.MqHandler.handleRequest(MqHandler.java:103)")
							.setWritingEndpointHandler(backendEndpointHandler)
						)
				.build())));
		
		// Request the shopping card from the db.
		timestamp = timestamp.plus(10, ChronoUnit.MILLIS);
		backendEndpointHandler.setHandlingTime(timestamp);
		String sqlRequestId = UUID.randomUUID().toString();
		assertTrue(sendEventToEtm("sql", this.sqlEventWriter.write(new SqlTelemetryEventBuilder()
				.setPayload("select * from shoppingcard where customerId = ?")
				.setId(sqlRequestId)
				.setDbQueryEventType(SqlEventType.SELECT)
				.setPayloadFormat(PayloadFormat.SQL)
				.addOrMergeEndpoint(new EndpointBuilder()
							.setName("TAB_CUSTOMER")
							.setWritingEndpointHandler(backendEndpointHandler)
						)
				.build())));
		timestamp = timestamp.plus(275, ChronoUnit.MILLIS);
		backendEndpointHandler.setHandlingTime(timestamp);
		assertTrue(sendEventToEtm("sql", this.sqlEventWriter.write(new SqlTelemetryEventBuilder()
				.setId(UUID.randomUUID().toString())
				.setCorrelationId(sqlRequestId)
				.setPayload("found 10 results")
				.setDbQueryEventType(SqlEventType.RESULTSET)
				.setPayloadFormat(PayloadFormat.SQL)
				.addOrMergeEndpoint(new EndpointBuilder()
							.setName("TAB_CUSTOMER")
							.addReadingEndpointHandler(backendEndpointHandler)
						)
				.build())));
		
		// Send a MQ response back to the gui app.
		timestamp = timestamp.plus(30, ChronoUnit.MILLIS);
		backendEndpointHandler.setHandlingTime(timestamp);
		timestamp = timestamp.plus(4, ChronoUnit.MILLIS);
		guiEndpointHandler.setHandlingTime(timestamp);
		assertTrue(sendEventToEtm("messaging", this.mqEventWriter.write(new MessagingTelemetryEventBuilder()
				.setId(UUID.randomUUID().toString())
				.setCorrelationId(mqMessageId)
				.setPayload("<shoppingcard_response><customer_id>543214</customer_id></shoppingcard_response>")
				.setPayloadFormat(PayloadFormat.XML)
				.setName("ShoppingCardResponse")
				.setMessagingEventType(MessagingEventType.RESPONSE)
				.setExpiry(timestamp.plusSeconds(30))
				.addOrMergeEndpoint(new EndpointBuilder()
							.setName("FRONTEND.QUEUE.1")
							.setWritingEndpointHandler(backendEndpointHandler)
							.addReadingEndpointHandler(guiEndpointHandler)
						)
				.build())));
		
		// And finally let the gui app return the html page.
		timestamp = timestamp.plus(32, ChronoUnit.MILLIS);
		guiEndpointHandler.setHandlingTime(timestamp);
		assertTrue(sendEventToEtm("http", this.httpEventWriter.write(new HttpTelemetryEventBuilder()
				.setId(UUID.randomUUID().toString())
				.setCorrelationId(eventId)
				.setPayload("<html><body><p>We found 2 items in your shopping card</p></body></html>")
				.setPayloadFormat(PayloadFormat.HTML)
				.setName("ReturnShoppingCard")
				.setHttpEventType(HttpEventType.RESPONSE)
				.addOrMergeEndpoint(new EndpointBuilder()
										.setWritingEndpointHandler(guiEndpointHandler)
									)
				.build())));
		
		// Now get the index page.
		getSecurePage(this.httpHost + "/gui/search/index.html", c -> ExpectedConditions.visibilityOf(findById("query-string")), 1000);

		// Now find the event and click on it.
	    waitForSearchResult(eventId).click();
	    // Wait for the endpoints tab to show up.
	    waitForShow("endpoint-tab-header");
	    // And select the endpoint tab
	    findById("endpoint-tab-header").click();
	    // Wait for the tab content to show up.
	    waitForShow("endpoint-overview");
	    
	    // Now select the canvas elements.
	    List<WebElement> canvasElements = findById("endpoint-overview").findElements(By.tagName("canvas"));
	    // Cytoscape renders 3 canvas elements. Make sure they are all present.
	    assertSame(3, canvasElements.size());
	    // Now click on the event reader
//	    WebElement canvas = canvasElements.get(0);
//	    
//	    // Calculate the clich point in the canvas.
//	    // The canvas consits of a grid with 3 columns. We have to click on the center of the 3th cell. 
//	    int clickPointXOffset = (canvas.getRect().width / 6) * 5;
//	    int clickPointYOffset = (canvas.getRect().height / 3) * 2;
	    
// TODO Onderstaande werkt niet met gecko driver...
//	    // Move to the event reader in the canvas and send a click event.
//	    new Actions(this.driver).moveToElement(canvas, clickPointXOffset, clickPointYOffset).click().perform();
//	    // And wait for the detail table to show.
//	    waitForShow("transaction-detail-table");
//	    // Make sure 6 events are in the transaction (and 1 for the table header). 
//	    List<WebElement> tableRows = findById("transaction-detail-table").findElements(By.tagName("tr"));
//	    assertSame(6 + 1, tableRows.size());
	}

}
