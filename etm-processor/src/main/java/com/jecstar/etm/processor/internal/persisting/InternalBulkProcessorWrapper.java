package com.jecstar.etm.processor.internal.persisting;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkProcessor.Listener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;

import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.domain.LogTelemetryEvent;
import com.jecstar.etm.domain.TelemetryEvent;
import com.jecstar.etm.domain.writers.json.BusinessTelemetryEventWriterJsonImpl;
import com.jecstar.etm.domain.writers.json.LogTelemetryEventWriterJsonImpl;
import com.jecstar.etm.processor.core.persisting.elastic.BusinessTelemetryEventPersister;
import com.jecstar.etm.processor.core.persisting.elastic.LogTelemetryEventPersister;
import com.jecstar.etm.server.core.configuration.EtmConfiguration;

/**
 * Bulk processor for ETM internal logging. Used for persisting events without
 * placing them on the ringbuffer. This is in particular useful for logging- and
 * business events.
 * 
 * @author Mark Holster
 */
public class InternalBulkProcessorWrapper implements Closeable {

	private static final LogTelemetryEventWriterJsonImpl logWriter = new LogTelemetryEventWriterJsonImpl();
	private static final BusinessTelemetryEventWriterJsonImpl businessWriter = new BusinessTelemetryEventWriterJsonImpl();
	
	private EtmConfiguration configuration;
	private BulkProcessor bulkProcessor;
	
	private LogTelemetryEventPersister logPersister;
	private BusinessTelemetryEventPersister businessPersister;
	
	private List<TelemetryEvent<?>> startupBuffer = new ArrayList<>();
	
	private boolean open = true;
	
	public void setConfiguration(EtmConfiguration configuration) {
		this.configuration = configuration;
		if (this.bulkProcessor != null) {
			if (this.logPersister == null) {
				this.logPersister = new LogTelemetryEventPersister(this.bulkProcessor, this.configuration);
			}
			if (this.businessPersister == null) {
				this.businessPersister = new BusinessTelemetryEventPersister(this.bulkProcessor, this.configuration);
			}
			flushStartupBuffer();
		}
	}

	public void setClient(Client elasticClient) {
		this.bulkProcessor = BulkProcessor.builder(elasticClient, new Listener() {
			@Override
			public void beforeBulk(long executionId, BulkRequest request) {
			}
			@Override
			public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
			}
			@Override
			public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
			}}
		)
		.setBulkActions(100)
		.setBulkSize(new ByteSizeValue(1, ByteSizeUnit.MB))
		.setFlushInterval(TimeValue.timeValueSeconds(10))
		.build();
		if (this.configuration != null) {
			if (this.logPersister == null) {
				this.logPersister = new LogTelemetryEventPersister(this.bulkProcessor, this.configuration);
			}
			if (this.businessPersister == null) {
				this.businessPersister = new BusinessTelemetryEventPersister(this.bulkProcessor, this.configuration);
			}
			flushStartupBuffer();			
		}
	}
	
	private synchronized void flushStartupBuffer() {
		for (TelemetryEvent<?> event : this.startupBuffer) {
			if (event instanceof LogTelemetryEvent) {
				persist((LogTelemetryEvent) event);
			}
		}
		this.startupBuffer.clear();
	}
	
	public void persist(LogTelemetryEvent event) {
		if (this.logPersister != null && isOpen()) {
			this.logPersister.persist(event, logWriter);
		} else {
			this.startupBuffer.add(event);
		}
	}
	
	public void persist(BusinessTelemetryEvent event) {
		if (this.businessPersister != null && isOpen()) {
			this.businessPersister.persist(event, businessWriter);
		} else {
			this.startupBuffer.add(event);
		}
	}
	
	private boolean isOpen() {
		return this.open;
	}

	@Override
	public void close() {
		this.open = false;
		if (this.bulkProcessor != null) {
			this.bulkProcessor.close();
		}
	}

	
	
}