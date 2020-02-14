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

package com.jecstar.etm.server.core.elasticsearch.builder;

import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.common.settings.Settings;

public class ClusterUpdateSettingsRequestBuilder extends AbstractActionRequestBuilder<ClusterUpdateSettingsRequest> {

    private final ClusterUpdateSettingsRequest request = new ClusterUpdateSettingsRequest();

    @Override
    public ClusterUpdateSettingsRequest build() {
        return this.request;
    }

    public ClusterUpdateSettingsRequestBuilder setPersistentSettings(Settings.Builder builder) {
        this.request.persistentSettings(builder);
        return this;
    }

    public ClusterUpdateSettingsRequestBuilder setTransientSettings(Settings.Builder builder) {
        this.request.transientSettings(builder);
        return this;
    }

}