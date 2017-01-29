package com.jecstar.etm.processor.ibmmq;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.MetricRegistry;
import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.ibmmq.configuration.Destination;
import com.jecstar.etm.processor.ibmmq.configuration.IbmMq;
import com.jecstar.etm.processor.ibmmq.configuration.QueueManager;

public class IbmMqProcessorImpl implements IbmMqProcessor {

	private final TelemetryCommandProcessor processor;
	private final MetricRegistry metricRegistry;
	private final String instanceName;
	private final String clusterName;
	private final IbmMq config;
	private ExecutorService executorService;

	public IbmMqProcessorImpl(TelemetryCommandProcessor processor, MetricRegistry metricRegistry, IbmMq config, String clusterName, String instanceName) {
		this.processor = processor;
		this.metricRegistry = metricRegistry;
		this.config = config;
		this.clusterName = clusterName;
		this.instanceName = instanceName;
	}
	
	public void start() {
		this.executorService = Executors.newFixedThreadPool(this.config.getTotalNumberOfListeners());
		for (QueueManager queueManager : this.config.getQueueManagers()) {
			for (Destination destination : queueManager.getDestinations()) {
				for (int i=0; i < destination.getNrOfListeners(); i++) {
					this.executorService.submit(new DestinationReader(this.clusterName + "_" + this.instanceName, this.processor, this.metricRegistry, queueManager, destination));
				}
			}
		}
	}

	public void stop() {
		if (this.executorService != null) {
			this.executorService.shutdownNow();
			try {
				this.executorService.awaitTermination(15, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			this.executorService = null;
		}
	}
}