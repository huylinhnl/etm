package com.holster.etm.gui.rest.repository;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.holster.etm.core.TelemetryEventType;

public class CorrelationData {

	public Map<String, String> data = new HashMap<String, String>(); 
	public Date validFrom;
	public Date validTill;
	public TelemetryEventType type;
	public boolean expired;

}
