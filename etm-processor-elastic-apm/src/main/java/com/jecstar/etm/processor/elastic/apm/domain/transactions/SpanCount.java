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

package com.jecstar.etm.processor.elastic.apm.domain.transactions;

import com.jecstar.etm.server.core.converter.JsonField;

public class SpanCount {

    @JsonField("started")
    private Long started;
    @JsonField("dropped")
    private Long dropped;

    /**
     * Number of correlated spans that are recorded.
     */
    public Long getStarted() {
        return this.started;
    }

    /**
     * Number of spans that have been dropped by the agent recording the transaction.
     */
    public Long getDropped() {
        return this.dropped;
    }
}