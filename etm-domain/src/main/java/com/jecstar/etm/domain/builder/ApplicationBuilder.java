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

package com.jecstar.etm.domain.builder;

import com.jecstar.etm.domain.Application;

import java.net.InetAddress;

public class ApplicationBuilder {

    private final Application application;

    public ApplicationBuilder() {
        this.application = new Application();
    }

    public Application build() {
        return this.application;
    }

    public ApplicationBuilder setName(String name) {
        this.application.name = name;
        return this;
    }

    public ApplicationBuilder setHostAddress(InetAddress hostAddress) {
        this.application.hostAddress = hostAddress;
        return this;
    }

    public ApplicationBuilder setInstance(String instance) {
        this.application.instance = instance;
        return this;
    }

    public ApplicationBuilder setPrincipal(String principal) {
        this.application.principal = principal;
        return this;
    }

    public ApplicationBuilder setVersion(String version) {
        this.application.version = version;
        return this;
    }
}
