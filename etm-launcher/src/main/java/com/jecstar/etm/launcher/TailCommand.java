package com.jecstar.etm.launcher;

import com.jecstar.etm.domain.writer.TelemetryEventTags;
import com.jecstar.etm.domain.writer.json.TelemetryEventTagsJsonImpl;
import com.jecstar.etm.launcher.configuration.Configuration;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

class TailCommand extends AbstractCommand {

    private Client elasticClient;

    private final TelemetryEventTags tags = new TelemetryEventTagsJsonImpl();
    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private final String timestampField = this.tags.getEndpointsTag() + "." + this.tags.getEndpointHandlersTag() + "." + this.tags.getEndpointHandlerHandlingTimeTag();

    void tail(Configuration configuration) {
        if (this.elasticClient != null) {
            return;
        }
        addShutdownHooks();
        this.elasticClient = createElasticsearchClient(configuration);
        SearchRequestBuilder builder = this.elasticClient.prepareSearch(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL)
                .setQuery(new BoolQueryBuilder().must(new MatchAllQueryBuilder()).filter(getFilterQuery()))
                .setFetchSource(getDisplayFields(), null)
                .setSize(50)
                .addSort(this.timestampField, SortOrder.DESC)
                .setTimeout(TimeValue.timeValueSeconds(30));
        SearchHits hits = builder.get().getHits();
        LastPrinted lastPrinted = new LastPrinted();
        if (hits != null) {
            for (int i = hits.getHits().length - 1; i >= 0; i--) {
                printSearchHit(hits.getAt(i), lastPrinted);
            }
        }
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (Thread.interrupted()) {
                break;
            }
            builder = this.elasticClient.prepareSearch(ElasticsearchLayout.EVENT_INDEX_ALIAS_ALL)
                    .setQuery(new BoolQueryBuilder().must(new MatchAllQueryBuilder()).filter(getFilterQuery().must(new RangeQueryBuilder(this.timestampField).gte(lastPrinted.timestamp))))
                    .setFetchSource(getDisplayFields(), null)
                    .addSort(this.timestampField, SortOrder.ASC)
                    .setTimeout(TimeValue.timeValueSeconds(30));
            ScrollableSearch searchHits = new ScrollableSearch(this.elasticClient, builder);
            boolean lastFound = false;
            for (SearchHit searchHit : searchHits) {
                if (searchHit.getId().equals(lastPrinted.id)) {
                    lastFound = true;
                    continue;
                }
                if (!lastFound) {
                    continue;
                }
                printSearchHit(searchHit, lastPrinted);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void printSearchHit(SearchHit hit, LastPrinted lastPrinted) {
        Map<String, Object> sourceMap = hit.getSourceAsMap();
        Map<String, Object> endpointMap = ((List<Map<String, Object>>) sourceMap.get(this.tags.getEndpointsTag())).get(0);
        Map<String, Object> endpointHandlerMap = ((List<Map<String, Object>>) endpointMap.get(this.tags.getEndpointHandlersTag())).get(0);
        Map<String, Object> applicationMap = (Map<String, Object>) endpointHandlerMap.get(this.tags.getEndpointHandlerApplicationTag());
        long timestamp = (long) endpointHandlerMap.get(this.tags.getEndpointHandlerHandlingTimeTag());
        System.out.println(df.format(new Date(timestamp))
                + " - " + applicationMap.get(this.tags.getApplicationInstanceTag())
                + " - " + sourceMap.get(this.tags.getLogLevelTag())
                + " - " + endpointMap.get(this.tags.getEndpointNameTag())
                + ": " + sourceMap.get(this.tags.getPayloadTag()));
        lastPrinted.id = hit.getId();
        lastPrinted.timestamp = timestamp;
    }

    private BoolQueryBuilder getFilterQuery() {
        return new BoolQueryBuilder()
                .must(new TermQueryBuilder(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.EVENT_OBJECT_TYPE_LOG))
                .must(new TermQueryBuilder(
                                this.tags.getEndpointsTag()
                                        + "." + this.tags.getEndpointHandlersTag()
                                        + "." + this.tags.getEndpointHandlerApplicationTag()
                                        + "." + this.tags.getApplicationNameTag()
                                        + ".keyword", "Enterprise Telemetry Monitor"
                        )
                );
    }

    private String[] getDisplayFields() {
        return new String[]{
                this.timestampField,
                this.tags.getEndpointsTag() + "." + this.tags.getEndpointHandlersTag() + "." + this.tags.getEndpointHandlerApplicationTag() + "." + this.tags.getApplicationInstanceTag(),
                this.tags.getEndpointsTag() + "." + this.tags.getEndpointNameTag(),
                this.tags.getLogLevelTag(),
                this.tags.getPayloadTag()
        };
    }

    private void addShutdownHooks() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (TailCommand.this.elasticClient != null) {
                try {
                    TailCommand.this.elasticClient.close();
                } catch (Throwable t) {
                }
            }
        }));
    }

    private class LastPrinted {

        private long timestamp;
        private String id;
    }
}