package com.jecstar.etm.server.core.domain.converter.json;

import com.jecstar.etm.domain.BusinessTelemetryEvent;
import com.jecstar.etm.domain.writer.json.BusinessTelemetryEventWriterJsonImpl;
import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.server.core.domain.converter.TelemetryEventConverter;

import java.util.Map;

public class BusinessTelemetryEventConverterJsonImpl extends BusinessTelemetryEventWriterJsonImpl implements TelemetryEventConverter<String, BusinessTelemetryEvent> {

    private final TelemetryEventJsonConverter<BusinessTelemetryEvent> converter = new TelemetryEventJsonConverter<>();

    @Override
    public String write(BusinessTelemetryEvent event, boolean includeId, boolean includePayloadEncoding) {
        return super.write(event, includeId, includePayloadEncoding);
    }

    @Override
    protected void doWrite(BusinessTelemetryEvent event, JsonBuilder builder) {
        this.converter.addDatabaseFields(event, builder);
        super.doWrite(event, builder);
    }

    @Override
    public BusinessTelemetryEvent read(String content, String id) {
        return read(this.converter.toMap(content), id);
    }

    @Override
    public void read(String content, BusinessTelemetryEvent event, String id) {
        read(this.converter.toMap(content), event, id);
    }

    @Override
    public BusinessTelemetryEvent read(Map<String, Object> valueMap, String id) {
        BusinessTelemetryEvent event = new BusinessTelemetryEvent();
        read(valueMap, event, id);
        return event;
    }

    @Override
    public void read(Map<String, Object> valueMap, BusinessTelemetryEvent event, String id) {
        this.converter.convert(valueMap, event, id);
    }

}
