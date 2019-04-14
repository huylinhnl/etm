package com.jecstar.etm.launcher.configuration;

import com.jecstar.etm.processor.ibmmq.configuration.IbmMq;
import com.jecstar.etm.processor.jms.configuration.Jms;
import com.jecstar.etm.processor.kafka.configuration.Kafka;
import com.jecstar.etm.signaler.configuration.Signaler;

public class Configuration {

    public String clusterName = "Enterprise Telemetry Monitor";
    public String instanceName = "Node_1";

    public String bindingAddress = "127.0.0.1";

    public String licenseUpdateUrl;

    public final Elasticsearch elasticsearch = new Elasticsearch();
    public final Http http = new Http();
    public final Logging logging = new Logging();
    public final Signaler signaler = new Signaler();

    public final IbmMq ibmMq = new IbmMq();
    public final Jms jms = new Jms();
    public final Kafka kafka = new Kafka();

    public boolean isHttpServerNecessary() {
        return this.http.restProcessorEnabled || this.http.guiEnabled;
    }

}
