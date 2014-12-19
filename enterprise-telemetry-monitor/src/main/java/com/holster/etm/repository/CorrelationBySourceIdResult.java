package com.holster.etm.repository;

import java.util.Date;
import java.util.UUID;

public class CorrelationBySourceIdResult {
	
	public UUID id;
	public UUID transactionId;
	public String transactionName;
	public Date creationTime = new Date(0);
	
	public CorrelationBySourceIdResult(UUID id, UUID transactionId, String transactionName, long creationTime) {
		this();
	    this.id = id;
	    this.transactionId = transactionId;
	    this.transactionName = transactionName;
	    this.creationTime.setTime(creationTime);
    }
	
	public CorrelationBySourceIdResult() {
	}

	public CorrelationBySourceIdResult initialize() {
		this.id = null;
		this.transactionId = null;
		this.transactionName = null;
		this.creationTime.setTime(0);
		return this;
	}

}
