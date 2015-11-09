package com.jecstar.etm.processor.processor;

import java.util.concurrent.ExecutorService;

import org.elasticsearch.client.Client;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.processor.TelemetryEvent;
import com.jecstar.etm.processor.repository.EndpointConfigResult;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class DisruptorEnvironment {

	private final Disruptor<TelemetryEvent> disruptor;
	private final TelemetryEventRepository telemetryEventRepository;

	public DisruptorEnvironment(final EtmConfiguration etmConfiguration, final ExecutorService executorService, final Client elasticClient, final PersistenceEnvironment persistenceEnvironment, final MetricRegistry metricRegistry) {
		this.disruptor = new Disruptor<TelemetryEvent>(TelemetryEvent::new, etmConfiguration.getEventBufferSize(), executorService, ProducerType.MULTI, new SleepingWaitStrategy());
		this.disruptor.handleExceptionsWith(new TelemetryEventExceptionHandler());
		int enhancingHandlerCount = etmConfiguration.getEnhancingHandlerCount();
		final EnhancingEventHandler[] enhancingEvntHandler = new EnhancingEventHandler[enhancingHandlerCount];
		this.telemetryEventRepository = persistenceEnvironment.createTelemetryEventRepository();
		for (int i = 0; i < enhancingHandlerCount; i++) {
			enhancingEvntHandler[i] = new EnhancingEventHandler(persistenceEnvironment.createTelemetryEventRepository(), i, enhancingHandlerCount, metricRegistry.timer("event-enhancing"));
		}
		
		int persistingHandlerCount = etmConfiguration.getPersistingHandlerCount();
		final PersistingEventHandler[] persistingEventHandlers = new PersistingEventHandler[persistingHandlerCount]; 
		for (int i = 0; i < persistingHandlerCount; i++) {
			persistingEventHandlers[i] = new PersistingEventHandler(persistenceEnvironment.createTelemetryEventRepository(), i, persistingHandlerCount, metricRegistry.timer("event-persisting"));
		}
		this.disruptor.handleEventsWith(enhancingEvntHandler);
		if (persistingEventHandlers.length > 0) {
			this.disruptor.after(enhancingEvntHandler).handleEventsWith(persistingEventHandlers);
		}
	}
	
	public RingBuffer<TelemetryEvent> start() {
		return this.disruptor.start();
	}

	public void shutdown() {
		this.disruptor.shutdown();
    }

	public void findEndpointConfig(String endpoint, EndpointConfigResult result) {
	    this.telemetryEventRepository.findEndpointConfig(endpoint, result);
    }
}
