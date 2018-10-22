package com.jecstar.etm.domain.writer.json;

import com.jecstar.etm.domain.*;
import com.jecstar.etm.domain.writer.TelemetryEventTags;
import com.jecstar.etm.domain.writer.TelemetryEventWriter;

import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Converter class that converts a <code>TelemetryEvent</code> to a JSON string.
 *
 * @author mark
 */
public abstract class AbstractJsonTelemetryEventWriter<Event extends TelemetryEvent<Event>> implements TelemetryEventWriter<String, Event> {

    private final TelemetryEventTags tags = new TelemetryEventTagsJsonImpl();
    final JsonWriter jsonWriter = new JsonWriter();

    @Override
    public String write(Event event) {
        return write(event, true, true);
    }

    protected String write(Event event, boolean includeId, boolean includePayloadEncoding) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean added = this.jsonWriter.addStringElementToJsonBuffer(this.tags.getObjectTypeTag(), getType(), sb, true);
        if (includeId) {
            added = this.jsonWriter.addStringElementToJsonBuffer(this.tags.getIdTag(), event.id, sb, !added) || added;
        }
        added = this.jsonWriter.addStringElementToJsonBuffer(this.tags.getCorrelationIdTag(), event.correlationId, sb, !added) || added;
        added = addMapElementToJsonBuffer(this.tags.getCorrelationDataTag(), event.correlationData, sb, !added) || added;
        if (event.endpoints.size() != 0) {
            if (added) {
                sb.append(", ");
            }
            sb.append(this.jsonWriter.escapeToJson(getTags().getEndpointsTag(), true)).append(": [");
            boolean endpointAdded = false;
            for (int i = 0; i < event.endpoints.size(); i++) {
                endpointAdded = addEndpointToJsonBuffer(event.endpoints.get(i), sb, i == 0 || !endpointAdded, getTags()) || endpointAdded;
            }
            sb.append("]");
            added = endpointAdded || added;
        }
        added = addMapElementToJsonBuffer(this.tags.getExtractedDataTag(), event.extractedData, sb, !added) || added;
        added = this.jsonWriter.addStringElementToJsonBuffer(this.tags.getNameTag(), event.name, sb, !added) || added;
        added = addMapElementToJsonBuffer(this.tags.getMetadataTag(), event.metadata, sb, !added) || added;
        added = this.jsonWriter.addStringElementToJsonBuffer(this.tags.getPayloadTag(), event.payload, sb, !added) || added;
        if (event.payloadEncoding != null) {
            added = this.jsonWriter.addStringElementToJsonBuffer(this.tags.getPayloadEncodingTag(), event.payloadEncoding.name(), sb, !added) || added;
        }
        if (event.payloadFormat != null) {
            added = this.jsonWriter.addStringElementToJsonBuffer(this.tags.getPayloadFormatTag(), event.payloadFormat.name(), sb, !added) || added;
        }
        if (event.payload != null) {
            added = this.jsonWriter.addIntegerElementToJsonBuffer(this.tags.getPayloadLengthTag(), event.payload.length(), sb, !added) || added;
        }
        added = doWrite(event, sb, !added) || added;
        sb.append("}");
        return sb.toString();
    }

    abstract String getType();

    abstract boolean doWrite(Event event, StringBuilder buffer, boolean firstElement);

    private boolean addMapElementToJsonBuffer(String elementName, Map<String, Object> elementValues, StringBuilder buffer, boolean firstElement) {
        if (elementValues.size() < 1) {
            return false;
        }
        if (!firstElement) {
            buffer.append(", ");
        }
        buffer.append(this.jsonWriter.escapeToJson(elementName, true)).append(": {");
        firstElement = true;
        for (Entry<String, Object> entry : elementValues.entrySet()) {
            if (!firstElement) {
                buffer.append(", ");
            }
            buffer.append(this.jsonWriter.escapeObjectToJsonNameValuePair(entry.getKey(), entry.getValue()));
            firstElement = false;
        }
        buffer.append("}");
        return true;
    }

    private boolean addEndpointToJsonBuffer(Endpoint endpoint, StringBuilder buffer, boolean firstElement, TelemetryEventTags tags) {
        boolean endpointHandlerSet = false;
        for (EndpointHandler handler : endpoint.getEndpointHandlers()) {
            if (handler.isSet()) {
                endpointHandlerSet = true;
                break;
            }
        }
        if (endpoint.name == null && !endpointHandlerSet) {
            return false;
        }
        if (!firstElement) {
            buffer.append(", ");
        }
        buffer.append("{");
        boolean added = this.jsonWriter.addStringElementToJsonBuffer(tags.getEndpointNameTag(), endpoint.name, buffer, true);
        if (endpointHandlerSet) {
            if (added) {
                buffer.append(", ");
            }
            buffer.append(this.jsonWriter.escapeToJson(getTags().getEndpointHandlersTag(), true)).append(": [");
            added = false;
            EndpointHandler writingEndpointHandler = endpoint.getWritingEndpointHandler();
            Instant latencyStart = null;
            if (writingEndpointHandler != null) {
                added = addEndpointHandlerToJsonBuffer(null, writingEndpointHandler, buffer, true, getTags()) || added;
                latencyStart = writingEndpointHandler.handlingTime;
            }

            for (EndpointHandler endpointHandler : endpoint.getReadingEndpointHandlers()) {
                added = addEndpointHandlerToJsonBuffer(latencyStart, endpointHandler, buffer, !added, getTags()) || added;
            }
            buffer.append("]");
            added = true;
        }
        buffer.append("}");
        return added;
    }

    private boolean addEndpointHandlerToJsonBuffer(Instant latencyStart, EndpointHandler endpointHandler, StringBuilder buffer, boolean firstElement, TelemetryEventTags tags) {
        if (!endpointHandler.isSet()) {
            return false;
        }
        if (!firstElement) {
            buffer.append(", ");
        }
        buffer.append("{");
        boolean added = this.jsonWriter.addStringElementToJsonBuffer(tags.getEndpointHandlerTypeTag(), endpointHandler.type.name(), buffer, true);
        if (endpointHandler.handlingTime != null) {
            added = this.jsonWriter.addInstantElementToJsonBuffer(tags.getEndpointHandlerHandlingTimeTag(), endpointHandler.handlingTime, buffer, !added) || added;
            if (latencyStart != null) {
                added = this.jsonWriter.addLongElementToJsonBuffer(tags.getEndpointHandlerLatencyTag(), endpointHandler.handlingTime.toEpochMilli() - latencyStart.toEpochMilli(), buffer, !added) || added;
            }
        }
        added = this.jsonWriter.addStringElementToJsonBuffer(this.tags.getEndpointHandlerTransactionIdTag(), endpointHandler.transactionId, buffer, !added) || added;
        added = this.jsonWriter.addIntegerElementToJsonBuffer(this.tags.getEndpointHandlerSequenceNumberTag(), endpointHandler.sequenceNumber, buffer, !added) || added;
        added = addMapElementToJsonBuffer(this.tags.getMetadataTag(), endpointHandler.metadata, buffer, !added) || added;
        Application application = endpointHandler.application;
        if (application.isSet()) {
            if (added) {
                buffer.append(", ");
            }
            buffer.append(this.jsonWriter.escapeToJson(tags.getEndpointHandlerApplicationTag(), true)).append(": {");
            added = this.jsonWriter.addStringElementToJsonBuffer(tags.getApplicationNameTag(), application.name, buffer, true);
            added = this.jsonWriter.addInetAddressElementToJsonBuffer(tags.getApplicationHostAddressTag(), tags.getApplicationHostNameTag(), application.hostAddress, buffer, !added) || added;
            added = this.jsonWriter.addStringElementToJsonBuffer(tags.getApplicationInstanceTag(), application.instance, buffer, !added) || added;
            added = this.jsonWriter.addStringElementToJsonBuffer(tags.getApplicationVersionTag(), application.version, buffer, !added) || added;
            added = this.jsonWriter.addStringElementToJsonBuffer(tags.getApplicationPrincipalTag(), application.principal, buffer, !added) || added;
            buffer.append("}");
        }
        Location location = endpointHandler.location;
        if (location.isSet()) {
            if (added) {
                buffer.append(", ");
            }
            buffer.append(this.jsonWriter.escapeToJson(tags.getEndpointHandlerLocationTag(), true)).append(": {");
            added = this.jsonWriter.addDoubleElementToJsonBuffer(tags.getLatitudeTag(), location.latitude, buffer, true);
            added = this.jsonWriter.addDoubleElementToJsonBuffer(tags.getLongitudeTag(), location.longitude, buffer, !added) || added;
            buffer.append("}");
        }
        buffer.append("}");
        return true;
    }

    @Override
    public TelemetryEventTags getTags() {
        return this.tags;
    }
}
