package com.jecstar.etm.domain.writer.json;

import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.writer.HttpTelemetryEventWriter;
import com.jecstar.etm.domain.writer.TelemetryEventTags;

public class HttpTelemetryEventWriterJsonImpl extends AbstractJsonTelemetryEventWriter<HttpTelemetryEvent> implements HttpTelemetryEventWriter<String> {

    @Override
    String getType() {
        return TelemetryEventTags.EVENT_OBJECT_TYPE_HTTP;
    }

    @Override
    protected boolean doWrite(HttpTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
        boolean added = !firstElement;
        added = this.jsonWriter.addInstantElementToJsonBuffer(getTags().getExpiryTag(), event.expiry, buffer, !added) || added;
        if (event.httpEventType != null) {
            added = this.jsonWriter.addStringElementToJsonBuffer(getTags().getHttpEventTypeTag(), event.httpEventType.name(), buffer, !added) || added;
        }
        added = this.jsonWriter.addIntegerElementToJsonBuffer(getTags().getStatusCodeTag(), event.statusCode, buffer, !added) || added;
        return added;
    }

}
