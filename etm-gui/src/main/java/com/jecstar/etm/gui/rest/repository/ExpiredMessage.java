package com.jecstar.etm.gui.rest.repository;

import java.util.Date;
import java.util.UUID;

public class ExpiredMessage {

	private UUID id;
	
	private String name;
	
	private String application;
	
	private Date startTime;
	
	private Date expirationTime;

	public ExpiredMessage(UUID id, String name, Date startTime, Date expirationTime, String application) {
		this.id = id;
	    this.name = name;
	    this.startTime = startTime;
	    this.expirationTime = expirationTime;
	    this.application = application;
    }

	public UUID getId() {
	    return this.id;
    }
	
	public String getName() {
		return this.name;
	}

	public String getApplication() {
		return this.application;
	}

	public Date getStartTime() {
		return this.startTime;
	}

	public Date getExpirationTime() {
		return this.expirationTime;
	}
}