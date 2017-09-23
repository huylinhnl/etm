package com.jecstar.etm.processor.ibmmq.configuration;

import java.util.ArrayList;
import java.util.List;

public class IbmMq {

	public boolean enabled = false;
	public final List<QueueManager> queueManagers = new ArrayList<>();
	
	public int getTotalNumberOfListeners() {
		if (this.queueManagers.isEmpty()) {
			return 0;
		}
		return this.queueManagers.stream().mapToInt(
				f -> f.getDestinations().stream().mapToInt(Destination::getNrOfListeners).sum()
		).sum();
	}
	
	public List<QueueManager> getQueueManagers() {
		return this.queueManagers;
	}
}
