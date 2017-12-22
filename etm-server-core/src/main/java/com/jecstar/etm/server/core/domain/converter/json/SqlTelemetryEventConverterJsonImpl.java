package com.jecstar.etm.server.core.domain.converter.json;

import com.jecstar.etm.domain.SqlTelemetryEvent;
import com.jecstar.etm.domain.SqlTelemetryEvent.SqlEventType;
import com.jecstar.etm.domain.writer.json.SqlTelemetryEventWriterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.TelemetryEventConverter;

import java.util.Map;

public class SqlTelemetryEventConverterJsonImpl extends SqlTelemetryEventWriterJsonImpl implements TelemetryEventConverter<String, SqlTelemetryEvent> {

	private final TelemetryEventJsonConverter<SqlTelemetryEvent> converter = new TelemetryEventJsonConverter<>();

    @Override
    public String write(SqlTelemetryEvent event, boolean includeId) {
        return super.write(event, includeId);
    }

    @Override
	protected boolean doWrite(SqlTelemetryEvent event, StringBuilder buffer, boolean firstElement) {
		boolean added = this.converter.addDatabaseFields(buffer, event, firstElement);
		return super.doWrite(event, buffer, !added);
	}

	@Override
	public SqlTelemetryEvent read(String content, String id) {
	    return read(this.converter.toMap(content), id);
	}
	
	@Override
	public void read(String content, SqlTelemetryEvent event, String id) {
		read(this.converter.toMap(content), event, id);
	}

	@Override
	public SqlTelemetryEvent read(Map<String, Object> valueMap, String id) {
		SqlTelemetryEvent event = new SqlTelemetryEvent();
		read(valueMap, event, id);
		return event;
	}

	@Override
	public void read(Map<String, Object> valueMap, SqlTelemetryEvent event, String id) {
		this.converter.convert(valueMap, event, id);
		event.sqlEventType =  SqlEventType.safeValueOf(this.converter.getString(getTags().getSqlEventTypeTag(), valueMap));
	}

}
