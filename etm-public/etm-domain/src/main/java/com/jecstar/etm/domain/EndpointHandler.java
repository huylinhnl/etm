package com.jecstar.etm.domain;

import java.time.ZonedDateTime;

public class EndpointHandler {

    /**
     * The <code>Application</code> that is handling the endpoint.
     */
    public Application application = new Application();

    /**
     * The <code>Location</code> the handling took place.
     */
    public Location location = new Location();

    /**
     * The time the handling took place.
     */
    public ZonedDateTime handlingTime;

    /**
     * The ID of the transaction this event belongs to. Events with the same
     * transactionId form and end-to-end chain within the application.
     */
    public String transactionId;

    /**
     * The sequenceNumber within this transactionId. When 2 events occur at the exact same <code>handlingTime</code>
     * the secuenceNumber can be used to determine which one happened first.
     */
    public Integer sequenceNumber;

    //READ ONLY FIELDS
    /**
     * The time between the write and read of the event. This value is only
     * filled when this <code>EndpointHandler</code> is a reading endpoint
     * handler on an <code>Endpoint</code>. Calculation of this value is done in
     * the persister, or in the elastic update script and will only be filled
     * here when the event is read from the database.
     */
    public Long latency;

    /**
     * The time between the handling time of a request and response. This value
     * is only filled when the event is a request (for example a messaging
     * request). The value is calculated by the elastic update script and will
     * only be filled here when the event is read from the database.
     */
    public Long responseTime;

    public EndpointHandler initialize() {
        this.application.initialize();
        this.location.initialize();
        this.handlingTime = null;
        this.transactionId = null;
        this.sequenceNumber = null;
        // Initialize read only fields.
        this.latency = null;
        this.responseTime = null;
        return this;
    }

    public EndpointHandler initialize(EndpointHandler copy) {
        initialize();
        if (copy == null) {
            return this;
        }
        this.application.initialize(copy.application);
        this.location.initialize(copy.location);
        this.handlingTime = copy.handlingTime;
        this.transactionId = copy.transactionId;
        this.sequenceNumber = copy.sequenceNumber;
        // Initialize read only fields.
        this.latency = copy.latency;
        this.responseTime = copy.responseTime;
        return this;
    }

    public boolean isSet() {
        return this.handlingTime != null || this.transactionId != null || this.location.isSet() || this.application.isSet();
    }

    public long getCalculatedHash() {
        long hash = 7;
        if (this.handlingTime != null) {
            hash = hash * 31 + this.handlingTime.toInstant().toEpochMilli();
        }
        if (this.transactionId != null) {
            for (int i = 0; i < this.transactionId.length(); i++) {
                hash = hash * 31 + this.transactionId.charAt(i);
            }
        }
        if (this.sequenceNumber != null) {
            hash = hash * 31 + sequenceNumber;
        }
        if (this.application.name != null) {
            for (int i = 0; i < this.application.name.length(); i++) {
                hash = hash * 31 + this.application.name.charAt(i);
            }
        }
        return hash;
    }

}