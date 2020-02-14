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

package com.jecstar.etm.server.core.domain.aggregator.metric;

import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.SumAggregationBuilder;

public class SumMetricsAggregator extends FieldBasedMetricsAggregator {

    public static final String TYPE = "sum";

    public SumMetricsAggregator() {
        super();
        setMetricsAggregatorType(TYPE);
    }

    @Override
    public SumMetricsAggregator clone() {
        SumMetricsAggregator clone = new SumMetricsAggregator();
        super.clone(clone);
        return clone;
    }

    @Override
    public SumAggregationBuilder toAggregationBuilder() {
        return AggregationBuilders.sum(getId()).setMetaData(getMetadata()).field(getField());
    }
}
