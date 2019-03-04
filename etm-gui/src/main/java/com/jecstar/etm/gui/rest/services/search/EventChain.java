package com.jecstar.etm.gui.rest.services.search;

import com.jecstar.etm.domain.EndpointHandler;
import com.jecstar.etm.domain.HttpTelemetryEvent;
import com.jecstar.etm.domain.MessagingTelemetryEvent;
import com.jecstar.etm.domain.writer.TelemetryEventTags;
import com.jecstar.etm.domain.writer.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.server.core.domain.principal.EtmPrincipal;
import com.jecstar.etm.server.core.util.LegacyEndpointHandler;
import org.elasticsearch.search.SearchHit;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class that holds all information to display the events that are correlated to each other and for a chain.
 */
public class EventChain {

    private final JsonConverter jsonConverter = new JsonConverter();
    private final TelemetryEventTags eventTags = new TelemetryEventTagsJsonImpl();

    private Set<String> transactionIds = new HashSet<>();
    private Set<String> eventIds = new HashSet<>();
    private List<Event> events = new ArrayList<>();

    public String toJson(EtmPrincipal etmPrincipal) {
        calculateAbsoluteTransactionPercentages();
        StringBuilder result = new StringBuilder();
        result.append("\"chart_config\": {");
        result.append("\"credits\": {\"enabled\": false}");
        result.append(", \"legend\": {\"enabled\": false}");
        result.append(", \"time\": {\"timezone\": " + this.jsonConverter.escapeToJson(etmPrincipal.getTimeZone().toZoneId().toString(), true) + "}");
        result.append(", \"chart\": {\"type\": \"xrange\"}");
        result.append(", \"title\": {\"text\": \"Event chain times\"}");
        result.append(", \"xAxis\": {\"type\": \"datetime\"}");
        result.append(", \"yAxis\": {\"title\": { \"text\": \"Events\"}, \"reversed\": true, \"categories\": [");
        result.append(this.events.stream().map(e -> this.jsonConverter.escapeToJson(e.name, true)).collect(Collectors.joining(",")));
        result.append("]}");
        result.append(", \"series\": [{\"name\": \"Chain overview\", \"pointPadding\": 0, \"colorByPoint\": false, \"colorIndex\": 7, \"data\": [");
        result.append(
                IntStream.range(0, this.events.size())
                        .mapToObj(i -> "{ \"x\": "
                                + this.events.get(i).startTime.toEpochMilli()
                                + ", \"x2\": " + (this.events.get(i).endTime != null ? this.events.get(i).endTime.toEpochMilli() : this.events.get(i).startTime.toEpochMilli() + 10)
                                + ",\"y\": " + i
                                + ",\"partialFill\": " + (this.events.get(i).absoluteTransactionPercentage != null ? this.events.get(i).absoluteTransactionPercentage.toString() : 0.0)
                                + ",\"dataLabels\": {\"enabled\": " + (this.events.get(i).absoluteTransactionPercentage != null ? "true" : "false") + "}"
                                + ",\"event_time\": " + this.jsonConverter.escapeToJson(this.events.get(i).getTotalEventTime() != null ? etmPrincipal.getNumberFormat().format(this.events.get(i).getTotalEventTime().toMillis()) : null, true)
                                + ",\"event_id\": " + this.jsonConverter.escapeToJson(this.events.get(i).id, true)
                                + ",\"event_type\": " + this.jsonConverter.escapeToJson(this.events.get(i).type, true)
                                + ",\"endpoint\": " + this.jsonConverter.escapeToJson(this.events.get(i).endpoint, true)
                                + ",\"application\": " + this.jsonConverter.escapeToJson(this.events.get(i).applicationName, true)
                                + ",\"transaction_id\": " + this.jsonConverter.escapeToJson(this.events.get(i).transactionId, true) + "}")
                        .collect(Collectors.joining(","))
        );
        result.append("], \"tooltip\": { \"pointFormat\": " + this.jsonConverter.escapeToJson("Name: <b>{point.yCategory}</b><br/>Application: <b>{point.application}</b><br/>Endpoint: <b>{point.endpoint}</b><br/>Response time: <b>{point.event_time}ms</b><br/>", true) + "}}]");
        result.append("}");
        return result.toString();
    }

    private void calculateAbsoluteTransactionPercentages() {
        if (this.events.size() == 0) {
            return;
        }
        this.events.sort(Comparator.comparing((Event c) -> c.startTime).thenComparing(c -> c.order).thenComparing(c -> c.endTime, Comparator.reverseOrder()));
        Duration totalEventTime = this.events.get(0).getTotalEventTime();
        if (totalEventTime == null) {
            return;
        }
        for (int i = 0; i < this.events.size(); i++) {
            Event event = this.events.get(i);
            Duration eventTime = event.getTotalEventTime();
            if (eventTime == null) {
                continue;
            }
            if (event.writer) {
                // A writer -> the percentage is calculated over the max latency of all readers of the same event.
                if (event.async) {
                    event.setAbsoluteTransactionPercentage(0);
                } else {
                    long lowestStartTime = event.startTime.toEpochMilli();
                    long highestEndTime = event.endTime.toEpochMilli();
                    Optional<Event> minEvent = this.events.stream()
                            .skip(i)
                            .filter(p -> !p.writer && Objects.equals(p.id, event.id) && p.startTime != null && !p.startTime.isBefore(event.startTime))
                            .min(Comparator.comparing(e -> e.startTime.toEpochMilli()));
                    if (minEvent.isPresent()) {
                        lowestStartTime = minEvent.get().startTime.toEpochMilli();
                    }

                    Optional<Event> maxEvent = this.events.stream()
                            .skip(i)
                            .filter(p -> !p.writer && Objects.equals(p.id, event.id) && p.endTime != null && !p.endTime.isAfter(event.endTime))
                            .max(Comparator.comparing(e -> e.endTime.toEpochMilli()));
                    if (maxEvent.isPresent()) {
                        highestEndTime = maxEvent.get().endTime.toEpochMilli();
                    }
                    final long latency = eventTime.toMillis() - (highestEndTime - lowestStartTime);
                    event.setAbsoluteTransactionPercentage((float) latency / (float) totalEventTime.toMillis());
                }
            } else {
                // A reader -> search for event from the same application that are written after the current event.
                long backendTime = this.events.stream()
                        .skip(i)
                        .filter(p -> p.writer && Objects.equals(p.transactionId, event.transactionId) && Objects.equals(p.applicationName, event.applicationName) && p.getTotalEventTime() != null && !p.async)
                        .mapToLong(e -> e.getTotalEventTime().toMillis())
                        .sum();
                Duration absoluteTime = eventTime.minusMillis(backendTime);
                event.setAbsoluteTransactionPercentage((float) absoluteTime.toMillis() / (float) totalEventTime.toMillis());
            }
        }
    }

    public boolean containsTransaction(String transactionId) {
        return this.transactionIds.contains(transactionId);
    }

    public void addTransactionId(String transactionId) {
        this.transactionIds.add(transactionId);
    }

    @LegacyEndpointHandler("Remove extraction of reading- and writing endpoint handlers")
    public Set<String> addSearchHit(SearchHit searchHit) {
        if (this.eventIds.contains(searchHit.getId())) {
            return Collections.emptySet();
        }
        Set<String> transactionIds = new HashSet<>();
        this.eventIds.add(searchHit.getId());
        Map<String, Object> source = searchHit.getSourceAsMap();
        Instant expiry = this.jsonConverter.getInstant(this.eventTags.getExpiryTag(), source);
        String subType = null;
        if ("http".equals(getEventType(searchHit))) {
            subType = this.jsonConverter.getString(this.eventTags.getHttpEventTypeTag(), source);
        } else if ("messaging".equals(getEventType(searchHit))) {
            subType = this.jsonConverter.getString(this.eventTags.getMessagingEventTypeTag(), source);
        }
        String eventName = this.jsonConverter.getString(this.eventTags.getNameTag(), source);
        boolean response = false;
        if (MessagingTelemetryEvent.MessagingEventType.RESPONSE.name().equals(subType) || HttpTelemetryEvent.HttpEventType.RESPONSE.name().equals(subType)) {
            response = true;
        }
        boolean async = MessagingTelemetryEvent.MessagingEventType.FIRE_FORGET.name().equals(subType);
        List<Map<String, Object>> endpoints = this.jsonConverter.getArray(this.eventTags.getEndpointsTag(), source, Collections.emptyList());
        for (Map<String, Object> endpoint : endpoints) {
            String endpointName = this.jsonConverter.getString(this.eventTags.getEndpointNameTag(), endpoint);
            if ("http".equals(getEventType(searchHit))) {
                eventName = subType + " " + determineUrlPath(endpointName);
            }
            List<Map<String, Object>> endpointHandlers = this.jsonConverter.getArray(this.eventTags.getEndpointHandlersTag(), endpoint);
            if (endpointHandlers != null) {
                for (Map<String, Object> eh : endpointHandlers) {
                    boolean writer = EndpointHandler.EndpointHandlerType.WRITER.name().equals(this.jsonConverter.getString(this.eventTags.getEndpointHandlerTypeTag(), eh));
                    String transactionId = this.jsonConverter.getString(this.eventTags.getEndpointHandlerTransactionIdTag(), eh);
                    if (transactionId != null) {
                        transactionIds.add(transactionId);
                    }
                    if (!response) {
                        Long responseTime = this.jsonConverter.getLong(this.eventTags.getEndpointHandlerResponseTimeTag(), eh);
                        Instant startTime = this.jsonConverter.getInstant(this.eventTags.getEndpointHandlerHandlingTimeTag(), eh);
                        Instant endTime = null;
                        if (responseTime != null) {
                            endTime = startTime.plusMillis(responseTime);
                        } else if (expiry != null) {
                            endTime = expiry;
                        }
                        String appName = null;
                        Map<String, Object> appMap = this.jsonConverter.getObject(this.eventTags.getEndpointHandlerApplicationTag(), eh);
                        if (appMap != null) {
                            appName = this.jsonConverter.getString(this.eventTags.getApplicationNameTag(), appMap);
                        }
                        this.events.add(new Event(searchHit.getId(), searchHit.getType(), transactionId, endpointName, eventName, appName, writer, startTime, endTime, async));
                    }
                }
            } else {
                Map<String, Object> weh = this.jsonConverter.getObject("writing_endpoint_handler", endpoint);
                if (weh != null) {
                    String transactionId = this.jsonConverter.getString(this.eventTags.getEndpointHandlerTransactionIdTag(), weh);
                    if (transactionId != null) {
                        transactionIds.add(transactionId);
                    }
                    if (!response) {
                        Long responseTime = this.jsonConverter.getLong(this.eventTags.getEndpointHandlerResponseTimeTag(), weh);
                        Instant startTime = this.jsonConverter.getInstant(this.eventTags.getEndpointHandlerHandlingTimeTag(), weh);
                        Instant endTime = null;
                        if (responseTime != null) {
                            endTime = startTime.plusMillis(responseTime);
                        } else if (expiry != null) {
                            endTime = expiry;
                        }
                        String appName = null;
                        Map<String, Object> appMap = this.jsonConverter.getObject(this.eventTags.getEndpointHandlerApplicationTag(), weh);
                        if (appMap != null) {
                            appName = this.jsonConverter.getString(this.eventTags.getApplicationNameTag(), appMap);
                        }
                        this.events.add(new Event(searchHit.getId(), searchHit.getType(), transactionId, endpointName, eventName, appName, true, startTime, endTime, async));
                    }
                }
                List<Map<String, Object>> readingEndpointHandlers = this.jsonConverter.getArray("reading_endpoint_handlers", endpoint);
                if (readingEndpointHandlers != null) {
                    for (Map<String, Object> reh : readingEndpointHandlers) {
                        String transactionId = this.jsonConverter.getString(this.eventTags.getEndpointHandlerTransactionIdTag(), reh);
                        if (!response) {
                            Long responseTime = this.jsonConverter.getLong(this.eventTags.getEndpointHandlerResponseTimeTag(), reh);
                            Instant startTime = this.jsonConverter.getInstant(this.eventTags.getEndpointHandlerHandlingTimeTag(), reh);
                            Instant endTime = null;
                            if (responseTime != null) {
                                endTime = startTime.plusMillis(responseTime);
                            } else if (expiry != null) {
                                endTime = expiry;
                            }
                            String appName = null;
                            Map<String, Object> appMap = this.jsonConverter.getObject(this.eventTags.getEndpointHandlerApplicationTag(), reh);
                            if (appMap != null) {
                                appName = this.jsonConverter.getString(this.eventTags.getApplicationNameTag(), appMap);
                            }
                            this.events.add(new Event(searchHit.getId(), searchHit.getType(), transactionId, endpointName, eventName, appName, false, startTime, endTime, async));
                        }
                        if (transactionId != null) {
                            transactionIds.add(transactionId);
                        }
                    }
                }
            }
        }
        return transactionIds;
    }

    private String determineUrlPath(String endpointName) {
        try {
            return new URL(endpointName).getPath();
        } catch (MalformedURLException e) {
            return endpointName;
        }
    }

    /**
     * Gives the event type of a <code>SearchHit</code> instance.
     *
     * @param searchHit The <code>SearchHit</code> to determine the event type for.
     * @return The event type, or <code>null</code> if the event type cannot determined.
     */
    private String getEventType(SearchHit searchHit) {
        if (searchHit == null) {
            return null;
        }
        if (ElasticsearchLayout.ETM_DEFAULT_TYPE.equals(searchHit.getType())) {
            return this.jsonConverter.getString(this.eventTags.getObjectTypeTag(), searchHit.getSourceAsMap());
        } else {
            return searchHit.getType();
        }
    }

    private class Event {

        private final String id;
        private final String type;
        private final String transactionId;
        private final String endpoint;
        private final String name;
        private final String applicationName;
        private final Instant startTime;
        private final Instant endTime;
        private final boolean async;
        private final int order;
        private final boolean writer;
        private BigDecimal absoluteTransactionPercentage;

        private Event(String id, String type, String transactionId, String endpoint, String name, String applicationName, boolean writer, Instant startTime, Instant endTime, boolean async) {
            this.id = id;
            this.type = type;
            this.transactionId = transactionId;
            this.endpoint = endpoint;
            this.name = name + (writer ? " (sent)" : " (received)");
            this.applicationName = applicationName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.async = async;
            this.writer = writer;
            this.order = writer ? 0 : 1;
        }

        private Duration getTotalEventTime() {
            if (this.startTime == null || this.endTime == null) {
                return null;
            }
            return Duration.ofMillis(this.endTime.toEpochMilli() - this.startTime.toEpochMilli());
        }

        private void setAbsoluteTransactionPercentage(float percentage) {
            this.absoluteTransactionPercentage = new BigDecimal(Float.toString(percentage));
            this.absoluteTransactionPercentage = this.absoluteTransactionPercentage.setScale(4, BigDecimal.ROUND_HALF_UP);
        }
    }
}