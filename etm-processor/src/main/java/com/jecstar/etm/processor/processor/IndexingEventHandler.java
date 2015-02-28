package com.jecstar.etm.processor.processor;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

import com.jecstar.etm.core.logging.LogFactory;
import com.jecstar.etm.core.logging.LogWrapper;
import com.jecstar.etm.processor.TelemetryEvent;
import com.lmax.disruptor.EventHandler;

public class IndexingEventHandler implements EventHandler<TelemetryEvent>, Closeable {

	/**
	 * The <code>LogWrapper</code> for this class.
	 */
	private static final LogWrapper log = LogFactory.getLogger(IndexingEventHandler.class);
	
	//TODO this should be configurable
	private final int nrOfDocumentsPerRequest = 250;
	private final SolrServer server;
	private final long ordinal;
	private final long numberOfConsumers;
	private final List<SolrInputDocument> documents = new ArrayList<SolrInputDocument>(this.nrOfDocumentsPerRequest);
	private int docIx = -1;
	
	public IndexingEventHandler(final SolrServer server, final long ordinal, final long numberOfConsumers) {
		this.server = server;
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
		for (int i=0; i < this.nrOfDocumentsPerRequest; i++) {
			this.documents.add(new SolrInputDocument());
		}
	}

	@Override
	public void onEvent(TelemetryEvent event, long sequence, boolean endOfBatch)
			throws Exception {
		if (event.ignore || (sequence % this.numberOfConsumers) != this.ordinal) {
			return;
		}
//		long start = System.nanoTime();
		this.docIx++;
		SolrInputDocument document = this.documents.get(this.docIx);
		document.clear();
		document.addField("id", event.id.toString());
		if (event.application != null) {
			document.addField("application", event.application);
		}
		if (event.content != null) {
			document.addField("content", event.content);
		}
		if (event.correlationId != null) {
			document.addField("correlationId", event.correlationId);
		}
		if (event.creationTime != null) {
			document.addField("creationTime", event.creationTime);
		}
		if (event.endpoint != null) {
			document.addField("endpoint", event.endpoint);
		}
		if (event.name != null) {
			document.addField("name", event.name);
		}
		if (event.sourceCorrelationId != null) {
			document.addField("sourceCorrelationId", event.sourceCorrelationId);
		}
		if (event.sourceId != null) {
			document.addField("sourceId", event.sourceId);
		}
		if (event.transactionId != null) {
			document.addField("transactionId", event.transactionId);
		}
		if (event.transactionName != null) {
			document.addField("transactionName", event.transactionName);
		}
		if (event.type != null) {
			document.addField("type", event.type.name());
		}
		if (event.retention != null) {
			document.addField("retention", event.retention);
		}
		if (this.docIx == this.nrOfDocumentsPerRequest - 1) {
			this.server.add(this.documents, 15000);
			this.docIx = -1;
		}
//		Statistics.indexingTime.addAndGet(System.nanoTime() - start);
	}

	@Override
    public void close() throws IOException {
		if (this.docIx != -1) {
			try {
				// TODO commitWithin time should be in configuration
	            this.server.add(this.documents.subList(0, this.docIx + 1), 60000);
            } catch (SolrServerException e) {
	            if (log.isErrorLevelEnabled()) {
	            	log.logErrorMessage("Unable to add documents to indexer.", e);
	            }
            }
		}
    }

}