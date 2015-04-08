package com.jecstar.etm.processor.processor;

import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.processor.EventCommand;
import com.jecstar.etm.processor.TelemetryEvent;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;
import com.lmax.disruptor.EventHandler;

public class PersistingEventHandler implements EventHandler<TelemetryEvent> {

	private final long ordinal;
	private final long numberOfConsumers;
	private final EtmConfiguration etmConfiguration;
	
	private TelemetryEventRepository telemetryEventRepository;
	
	public PersistingEventHandler(final TelemetryEventRepository telemetryEventRepository, final long ordinal, final long numberOfConsumers, final EtmConfiguration etmConfiguration) {
		this.telemetryEventRepository = telemetryEventRepository;
	    this.ordinal = ordinal;
	    this.numberOfConsumers = numberOfConsumers;
	    this.etmConfiguration = etmConfiguration;
	}

	@Override
	public void onEvent(TelemetryEvent event, long sequence, boolean endOfBatch) throws Exception {
		if (event.ignore) {
			return;
		}
		if (!EventCommand.PROCESS.equals(event.eventCommand) || (sequence % this.numberOfConsumers) != this.ordinal) {
			return;
		}
//		long start = System.nanoTime();
		this.telemetryEventRepository.persistTelemetryEvent(event, this.etmConfiguration.getStatisticsTimeUnit());
//		Statistics.persistingTime.addAndGet(System.nanoTime() - start);
	}

}
