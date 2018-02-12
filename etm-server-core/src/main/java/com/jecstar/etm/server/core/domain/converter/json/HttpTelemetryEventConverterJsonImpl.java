package com.jecstar.etm.server.core.domain.converter.json;

import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.HttpTelemetryEvent.HttpEventType;
import com.jecstar.etm.domain.writer.json.HttpTelemetryEventWriterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.TelemetryEventConverter;

import java.util.Map;

public class HttpTelemetryEventConverterJsonImpl extends HttpTelemetryEventWriterJsonImpl implements TelemetryEventConverter<String, HttpTelemetryEvent> {

    private final TelemetryEventJsonConverter<HttpTelemetryEvent> converter = new TelemetryEventJsonConverter<>();

    @Override
    public String write(HttpTelemetryEvent event, boolean includeId, boolean includePayloadEncoding) {
        return super.write(event, includeId, includePayloadEncoding);
    }

    @Override
    protected boolean doWrite(HttpTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
        boolean added = this.converter.addDatabaseFields(buffer, event, firstElement);
        return super.doWrite(event, buffer, !added);
    }

    @Override
    public HttpTelemetryEvent read(String content, String id) {
        return read(this.converter.toMap(content), id);
    }

    @Override
    public void read(String content, HttpTelemetryEvent event, String id) {
        read(this.converter.toMap(content), event, id);
    }

    @Override
    public HttpTelemetryEvent read(Map<String, Object> valueMap, String id) {
        HttpTelemetryEvent event = new HttpTelemetryEvent();
        read(valueMap, event, id);
        return event;
    }

    @Override
    public void read(Map<String, Object> valueMap, HttpTelemetryEvent event, String id) {
        this.converter.convert(valueMap, event, id);
        event.expiry = this.converter.getZonedDateTime(getTags().getExpiryTag(), valueMap);
        event.httpEventType = HttpEventType.safeValueOf(this.converter.getString(getTags().getHttpEventTypeTag(), valueMap));
    }

}
