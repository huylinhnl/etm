/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.server.core.domain.converter.json;

import com.jecstar.etm.domain.SqlTelemetryEvent;
import com.jecstar.etm.domain.SqlTelemetryEvent.SqlEventType;
import com.jecstar.etm.domain.writer.json.JsonBuilder;
import com.jecstar.etm.domain.writer.json.SqlTelemetryEventWriterJsonImpl;
import com.jecstar.etm.server.core.domain.converter.TelemetryEventConverter;

import java.util.Map;

public class SqlTelemetryEventConverterJsonImpl extends SqlTelemetryEventWriterJsonImpl implements TelemetryEventConverter<String, SqlTelemetryEvent> {

    private final TelemetryEventJsonConverter<SqlTelemetryEvent> converter = new TelemetryEventJsonConverter<>();

    @Override
    public String write(SqlTelemetryEvent event, boolean includeId, boolean includePayloadEncoding) {
        return super.write(event, includeId, includePayloadEncoding);
    }

    @Override
    protected void doWrite(SqlTelemetryEvent event, JsonBuilder builder) {
        this.converter.addDatabaseFields(event, builder);
        super.doWrite(event, builder);
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
        event.sqlEventType = SqlEventType.safeValueOf(this.converter.getString(getTags().getSqlEventTypeTag(), valueMap));
    }

}
