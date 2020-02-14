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

package com.jecstar.etm.server.core.elasticsearch.domain;

import com.jecstar.etm.server.core.converter.JsonField;
import com.jecstar.etm.server.core.elasticsearch.domain.converter.DocsStatsConverter;
import com.jecstar.etm.server.core.elasticsearch.domain.converter.IndexingStatsConverter;
import com.jecstar.etm.server.core.elasticsearch.domain.converter.SearchStatsConverter;
import com.jecstar.etm.server.core.elasticsearch.domain.converter.StoreStatsConverter;

public class CommonStats {

    @JsonField(value = "docs", converterClass = DocsStatsConverter.class)
    private DocsStats docs;
    @JsonField(value = "store", converterClass = StoreStatsConverter.class)
    private StoreStats store;
    @JsonField(value = "indexing", converterClass = IndexingStatsConverter.class)
    private IndexingStats indexing;
    @JsonField(value = "search", converterClass = SearchStatsConverter.class)
    private SearchStats search;

    public DocsStats getDocs() {
        return this.docs;
    }

    public StoreStats getStore() {
        return this.store;
    }

    public IndexingStats getIndexing() {
        return this.indexing;
    }

    public SearchStats getSearch() {
        return this.search;
    }
}
