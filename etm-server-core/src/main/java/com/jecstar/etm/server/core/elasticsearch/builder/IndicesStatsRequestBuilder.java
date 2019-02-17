package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.client.Request;
import org.elasticsearch.common.unit.TimeValue;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class IndicesStatsRequestBuilder {

    private enum Feature {STORE, DOCS, INDEXING, SEARCH}

    private String[] indices;
    private Set<Feature> features = new HashSet<>();
    private TimeValue timeout;

    public IndicesStatsRequestBuilder setIndices(String... indices) {
        this.indices = indices;
        return this;
    }

    public IndicesStatsRequestBuilder clear() {
        setStore(false);
        setDocs(false);
        setIndexing(false);
        setSearch(false);
        return this;
    }

    public IndicesStatsRequestBuilder setStore(boolean store) {
        if (store) {
            this.features.add(Feature.STORE);
        } else {
            this.features.remove(Feature.STORE);
        }
        return this;
    }

    public IndicesStatsRequestBuilder setDocs(boolean docs) {
        if (docs) {
            this.features.add(Feature.DOCS);
        } else {
            this.features.remove(Feature.DOCS);
        }
        return this;
    }

    public IndicesStatsRequestBuilder setIndexing(boolean indexing) {
        if (indexing) {
            this.features.add(Feature.INDEXING);
        } else {
            this.features.remove(Feature.INDEXING);
        }
        return this;
    }

    public IndicesStatsRequestBuilder setSearch(boolean search) {
        if (search) {
            this.features.add(Feature.SEARCH);
        } else {
            this.features.remove(Feature.SEARCH);
        }
        return this;
    }

    public IndicesStatsRequestBuilder setTimeout(TimeValue timeValue) {
        this.timeout = timeValue;
        return this;
    }

    public TimeValue getTimeout() {
        return this.timeout;
    }

    public Request request() {
        return new Request("GET", getEndpoint());
    }

    private String getEndpoint() {
        StringBuilder endpoint = new StringBuilder();
        endpoint.append("/");
        if (this.indices != null && this.indices.length > 0) {
            String indices = String.join(",", this.indices) + "/";
            endpoint.append(indices);
        }
        endpoint.append("_stats");
        if (this.features.size() > 0) {
            String features = "/" + this.features.stream().map(f -> f.name().toLowerCase()).collect(Collectors.joining(","));
            endpoint.append(features);
        }
        return endpoint.toString();
    }
}
