package com.jecstar.etm.launcher.background;

import com.jecstar.etm.launcher.http.session.ElasticsearchSessionTags;
import com.jecstar.etm.launcher.http.session.ElasticsearchSessionTagsJsonImpl;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.BulkRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.DeleteRequestBuilder;
import com.jecstar.etm.server.core.elasticsearch.builder.SearchRequestBuilder;
import com.jecstar.etm.server.core.logging.LogFactory;
import com.jecstar.etm.server.core.logging.LogWrapper;
import com.jecstar.etm.server.core.persisting.ScrollableSearch;
import com.jecstar.etm.server.core.rest.AbstractJsonService;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

public class HttpSessionCleaner extends AbstractJsonService implements Runnable {

    /**
     * The <code>LogWrapper</code> for this class.
     */
    private static final LogWrapper log = LogFactory.getLogger(HttpSessionCleaner.class);

    private final EtmConfiguration etmConfiguration;
    private final DataRepository dataRepository;
    private final ElasticsearchSessionTags tags = new ElasticsearchSessionTagsJsonImpl();


    public HttpSessionCleaner(final EtmConfiguration etmConfiguration, final DataRepository dataRepository) {
        this.etmConfiguration = etmConfiguration;
        this.dataRepository = dataRepository;
    }

    @Override
    public void run() {
        if (log.isDebugLevelEnabled()) {
            log.logDebugMessage("Looking for expired http sessions to remove.");
        }
        try {
            // Not using a delete by query because it isn't able to delete in a certain type (by the Java api) and cannot delete documents with version equals to zero.
            SearchRequestBuilder searchRequestBuilder = enhanceRequest(
                    new SearchRequestBuilder().setIndices(ElasticsearchLayout.STATE_INDEX_NAME).setTypes(ElasticsearchLayout.ETM_DEFAULT_TYPE),
                    etmConfiguration
            )
                    .setFetchSource(false)
                    .setQuery(
                            QueryBuilders.boolQuery().must(
                                    QueryBuilders.rangeQuery(this.tags.getLastAccessedTag()).lt(System.currentTimeMillis() - this.etmConfiguration.getSessionTimeout()).from(0L)
                            ).filter(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.STATE_OBJECT_TYPE_SESSION))
                    );
            ScrollableSearch scrollableSearch = new ScrollableSearch(dataRepository, searchRequestBuilder);
            if (!scrollableSearch.hasNext()) {
                return;
            }
            if (log.isInfoLevelEnabled()) {
                log.logInfoMessage("Found " + scrollableSearch.getNumberOfHits() + " expired http sessions marked for deletion.");
            }
            BulkRequestBuilder bulkRequestBuilder = enhanceRequest(new BulkRequestBuilder(), etmConfiguration);
            for (SearchHit searchHit : scrollableSearch) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                bulkRequestBuilder.add(
                        enhanceRequest(
                                new DeleteRequestBuilder(searchHit.getIndex(), searchHit.getType(), searchHit.getId()),
                                etmConfiguration
                        ).build()
                );
                if (bulkRequestBuilder.getNumberOfActions() >= 50) {
                    this.dataRepository.bulk(bulkRequestBuilder);
                    bulkRequestBuilder = enhanceRequest(new BulkRequestBuilder(), etmConfiguration);
                }
            }
            if (bulkRequestBuilder.getNumberOfActions() > 0) {
                this.dataRepository.bulk(bulkRequestBuilder);
            }
            if (log.isInfoLevelEnabled()) {
                log.logInfoMessage(scrollableSearch.getNumberOfHits() + " expired http sessions deleted.");
            }
        } catch (Exception e) {
            if (log.isErrorLevelEnabled()) {
                log.logErrorMessage("Failed to clean http sessions", e);
            }
        }
    }

}
