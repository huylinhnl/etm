package com.jecstar.etm.processor.processor;

import java.io.Closeable;
import java.io.IOException;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.jecstar.etm.core.TelemetryCommand;
import com.jecstar.etm.core.TelemetryMessageEvent;
import com.jecstar.etm.core.configuration.EtmConfiguration;
import com.jecstar.etm.processor.repository.TelemetryEventRepository;
import com.lmax.disruptor.EventHandler;

public class PersistingEventHandler implements EventHandler<TelemetryCommand>, Closeable {

	private final long ordinal;
	private final long numberOfConsumers;
	private final EtmConfiguration etmConfiguration;
	private final Timer timer;

	private TelemetryEventRepository telemetryEventRepository;

	public PersistingEventHandler(final TelemetryEventRepository telemetryEventRepository, final long ordinal,
	        final long numberOfConsumers, final EtmConfiguration etmConfiguration, final Timer timer) {
		this.telemetryEventRepository = telemetryEventRepository;
		this.ordinal = ordinal;
		this.numberOfConsumers = numberOfConsumers;
		this.etmConfiguration = etmConfiguration;
		this.timer = timer;
	}

	@Override
	public void onEvent(TelemetryCommand command, long sequence, boolean endOfBatch) throws Exception {
		if (sequence % this.numberOfConsumers != this.ordinal) {
			return;
		}
		switch (command.commandType) {
		case MESSAGE_EVENT:
			persistTelemetryMessageEvent(command.messageEvent);
			break;
		default:
			break;
		}
	}

	private void persistTelemetryMessageEvent(TelemetryMessageEvent event) {
		final Context timerContext = this.timer.time();
		try {
			this.telemetryEventRepository.persistTelemetryMessageEvent(event, this.etmConfiguration.getStatisticsTimeUnit());
		} finally {
			timerContext.stop();
		}

	}

	@Override
    public void close() throws IOException {
		this.telemetryEventRepository.close();
    }

}
