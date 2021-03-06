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

package com.jecstar.etm.server.core;

import com.jecstar.etm.server.core.domain.configuration.EtmConfiguration;
import com.jecstar.etm.server.core.elasticsearch.DataRepository;
import com.jecstar.etm.server.core.elasticsearch.builder.GetRequestBuilder;
import org.apache.http.HttpHost;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.function.BiConsumer;

/**
 * Super class for all integration tests. This class requires a running
 * Elasticsearch instance on localhost:9300. ETM templates and scripts should be
 * configured on that instance as well.
 *
 * @author Mark Holster
 */
public abstract class AbstractIntegrationTest {

    protected final EtmConfiguration etmConfiguration = new EtmConfiguration("integration-test");
    protected BulkProcessor bulkProcessor;
    private DataRepository dataRepository;

    protected abstract BulkProcessor.Listener createBulkListener();

    @BeforeEach
    public void setup() {
        this.etmConfiguration.setEventBufferSize(1);
        RestHighLevelClient highLevelClient = new RestHighLevelClient(RestClient.builder(HttpHost.create("127.0.0.1:9200")));
        this.dataRepository = new DataRepository(highLevelClient);

        BiConsumer<BulkRequest, ActionListener<BulkResponse>> bulkConsumer =
                (request, bulkListener) ->
                        highLevelClient.bulkAsync(request, RequestOptions.DEFAULT, bulkListener);
        this.bulkProcessor = BulkProcessor.builder(bulkConsumer, createBulkListener())
                .setBulkActions(1)
                .build();
    }

    @AfterEach
    public void tearDown() {
        if (this.bulkProcessor != null) {
            this.bulkProcessor.close();
        }
    }

    protected GetResponse waitFor(String index, String id) throws InterruptedException {
        return waitFor(index, id, null);
    }

    protected GetResponse waitFor(String index, String id, Long version) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        do {
            GetResponse getResponse = this.dataRepository.get(new GetRequestBuilder(index, id));
            if (getResponse.isExists()) {
                if (version == null || getResponse.getVersion() == version) {
                    return getResponse;
                }
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedException();
            }
        } while (System.currentTimeMillis() - startTime < 10_000);
        throw new NoSuchEventException(index, id, version);
    }

}
