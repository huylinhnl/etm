package com.holster.etm.processor.mdb;

import java.util.Date;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;

import com.holster.etm.processor.TelemetryEvent;
import com.holster.etm.processor.TelemetryEventDirection;
import com.holster.etm.processor.TelemetryEventType;

@XmlRootElement(name = "TelemetryEvent")
public class XmlTelemetryEvent {

	@XmlEnum
	public enum Direction {
		@XmlEnumValue("INCOMING")
		INCOMING, 
		@XmlEnumValue("OUTGOING")
		OUTGOING
	}
	
	@XmlEnum
	public enum Type {
		@XmlEnumValue("MESSAGE_REQUEST")
		MESSAGE_REQUEST,
		@XmlEnumValue("MESSAGE_RESPONSE")
		MESSAGE_RESPONSE, 
		@XmlEnumValue("MESSAGE_DATAGRRAM")
		MESSAGE_DATAGRAM
	}
	
	@XmlElement
	public String application;
	
	@XmlElement
	public String content;

	@XmlElement
	public Date creationTime;
	
	@XmlElement
	public Direction direction;

	@XmlElement
	public String endpoint;

	@XmlElement
	public Date expiryTime;

	@XmlElement
	public String name;

	@XmlElement
	public String sourceCorrelationId;
	
	@XmlElement
	public String sourceId;

	@XmlElement
	public String transactionName;

	@XmlElement
	public Type type;

	public void copyToTelemetryEvent(TelemetryEvent telemetryEvent) {
	    telemetryEvent.application = this.application;
	    telemetryEvent.content = this.content;
	    if (this.creationTime != null) {
	    	telemetryEvent.creationTime.setTime(this.creationTime.getTime());
	    }
	    if (this.direction != null) {
	    	telemetryEvent.direction = TelemetryEventDirection.valueOf(this.direction.name());
	    }
	    telemetryEvent.endpoint = this.endpoint;
	    if (this.expiryTime != null) {
	    	telemetryEvent.expiryTime.setTime(this.expiryTime.getTime());
	    }
	    telemetryEvent.name = this.name;
	    telemetryEvent.sourceCorrelationId = this.sourceCorrelationId;
	    telemetryEvent.sourceId = this.sourceId;
	    telemetryEvent.transactionName = this.transactionName;
	    if (this.type != null) {
	    	telemetryEvent.type = TelemetryEventType.valueOf(this.type.name());
	    }
    }

}
