package com.jecstar.etm.server.core.domain.converter.json;

import com.jecstar.etm.domain.LogTelemetryEvent;
import com.jecstar.etm.domain.writer.json.LogTelemetryEventWriterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.TelemetryEventConverter;

import java.util.Map;

public class LogTelemetryEventConverterJsonImpl extends LogTelemetryEventWriterJsonImpl implements TelemetryEventConverter<String, LogTelemetryEvent> {

	private final TelemetryEventJsonConverter<LogTelemetryEvent> converter = new TelemetryEventJsonConverter<>();

    @Override
    public String write(LogTelemetryEvent event, boolean includeId) {
        return super.write(event, includeId);
    }

    @Override
	protected boolean doWrite(LogTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
		boolean added = this.converter.addDatabaseFields(buffer, event, firstElement);
		return super.doWrite(event, buffer, !added);
	}

	@Override
	public LogTelemetryEvent read(String content, String id) {
		return read(this.converter.toMap(content), id);
	}
	
	@Override
	public void read(String content, LogTelemetryEvent event, String id) {
		read(this.converter.toMap(content), event, id);
	}

	@Override
	public LogTelemetryEvent read(Map<String, Object> valueMap, String id) {
		LogTelemetryEvent event = new LogTelemetryEvent();
		read(valueMap, event, id);
		return event;
	}

	@Override
	public void read(Map<String, Object> valueMap, LogTelemetryEvent event, String id) {
		this.converter.convert(valueMap, event, id);
		event.logLevel = this.converter.getString(getTags().getLogLevelTag(), valueMap);
		event.stackTrace = this.converter.getString(getTags().getStackTraceTag(), valueMap);
	}

}
