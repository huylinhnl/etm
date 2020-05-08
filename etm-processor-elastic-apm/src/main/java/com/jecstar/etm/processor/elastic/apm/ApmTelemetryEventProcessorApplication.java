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

package com.jecstar.etm.processor.elastic.apm;

import com.jecstar.etm.processor.core.TelemetryCommandProcessor;
import com.jecstar.etm.processor.elastic.apm.configuration.ElasticApm;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

public class ApmTelemetryEventProcessorApplication extends Application {

    public ApmTelemetryEventProcessorApplication(TelemetryCommandProcessor processor, ElasticApm elasticApmProcessor) {
        ApmTelemetryEventProcessor.initialize(processor, elasticApmProcessor);
    }

    @Override
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<>();
        classes.add(ApmTelemetryEventProcessor.class);
        return classes;
    }
}