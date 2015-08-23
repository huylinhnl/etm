package com.jecstar.etm.processor.processor;

import java.nio.channels.IllegalSelectorException;
import java.util.concurrent.ExecutorService;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.EtmException;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.processor.TelemetryCommand;
import com.jecstar.etm.processor.TelemetryCommand.CommandType;
import com.lmax.disruptor.RingBuffer;

public class TelemetryCommandProcessor {
	
	private RingBuffer<TelemetryCommand> ringBuffer;
	private boolean started = false;
	
	private ExecutorService executorService;
	private EtmConfiguration etmConfiguration;
	private DisruptorEnvironment disruptorEnvironment;
	private PersistenceEnvironment persistenceEnvironment;
	private MetricRegistry metricRegistry;
	private Timer offerTimer;
	

	public void start(final ExecutorService executorService, final PersistenceEnvironment persistenceEnvironment, final EtmConfiguration etmConfiguration, final MetricRegistry metricRegistry) {
		if (this.started) {
			throw new IllegalStateException();
		}
		this.started = true;
		this.executorService = executorService;
		this.persistenceEnvironment = persistenceEnvironment;
		this.etmConfiguration = etmConfiguration;
		this.metricRegistry = metricRegistry;
		this.offerTimer = this.metricRegistry.timer("event-offering");
		this.disruptorEnvironment = new DisruptorEnvironment(etmConfiguration, executorService, this.persistenceEnvironment, this.metricRegistry);
		this.ringBuffer = this.disruptorEnvironment.start();
	}
	
	public void hotRestart() {
		if (!this.started) {
			throw new IllegalStateException();
		}
		DisruptorEnvironment newDisruptorEnvironment = new DisruptorEnvironment(this.etmConfiguration, this.executorService, this.persistenceEnvironment, this.metricRegistry);
		RingBuffer<TelemetryCommand> newRingBuffer = newDisruptorEnvironment.start();
		DisruptorEnvironment oldDisruptorEnvironment = this.disruptorEnvironment;
		
		this.ringBuffer = newRingBuffer;
		this.disruptorEnvironment = newDisruptorEnvironment;
		oldDisruptorEnvironment.shutdown();
	}
	
	public void stop() {
		if (!this.started) {
			throw new IllegalSelectorException();
		}
		this.disruptorEnvironment.shutdown();
	}
	
	public void stopAll() {
		if (!this.started) {
			throw new IllegalSelectorException();
		}		
		this.executorService.shutdown();
		this.disruptorEnvironment.shutdown();
		this.persistenceEnvironment.close();
	}


	public void processTelemetryEvent(final TelemetryCommand telemetryCommand) {
		if (!this.started) {
			throw new IllegalSelectorException();
		}
		if (this.etmConfiguration.getLicenseExpriy().getTime() < System.currentTimeMillis()) {
			throw new EtmException(EtmException.LICENSE_EXPIRED_EXCEPTION);
		}
		final Context timerContext = this.offerTimer.time();
		TelemetryCommand target = null;
		long sequence = this.ringBuffer.next();
		try {
			target = this.ringBuffer.get(sequence);
			target.initialize(telemetryCommand);
			preProcess(target);
		} finally {
			this.ringBuffer.publish(sequence);
			timerContext.stop();
		}
	}
	
	
	public MetricRegistry getMetricRegistry() {
	    return this.metricRegistry;
    }
	
	private void preProcess(TelemetryCommand command) {
		if (CommandType.EVENT.equals(command.commandType)) {
//			if (command.messageEvent.id == null) {
//				command.messageEvent.id = UUIDs.timeBased().toString();
//			}
//			this.persistenceEnvironment.getProcessingMap().addTelemetryEvent(command.messageEvent);
		}
	}
}
