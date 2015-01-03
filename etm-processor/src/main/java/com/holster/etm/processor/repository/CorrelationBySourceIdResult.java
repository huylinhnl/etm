package com.holster.etm.processor.repository;

import java.util.Date;
import java.util.UUID;

public class CorrelationBySourceIdResult {
	
	public UUID id;
	public UUID transactionId;
	public String transactionName;
	public String name;
	public Date creationTime = new Date(0);
	public Date expiryTime = new Date(0);
	
	public CorrelationBySourceIdResult(UUID id, String name, UUID transactionId, String transactionName, long creationTime, long expiryTime) {
		this();
	    this.id = id;
	    this.name = name;
	    this.transactionId = transactionId;
	    this.transactionName = transactionName;
	    this.creationTime.setTime(creationTime);
	    this.expiryTime.setTime(expiryTime);
    }
	
	public CorrelationBySourceIdResult() {
	}

	public CorrelationBySourceIdResult initialize() {
		this.id = null;
		this.name = null;;
		this.transactionId = null;
		this.transactionName = null;
		this.creationTime.setTime(0);
		this.expiryTime.setTime(0);
		return this;
	}

}
