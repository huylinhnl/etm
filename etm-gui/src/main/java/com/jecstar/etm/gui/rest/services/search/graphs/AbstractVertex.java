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

package com.jecstar.etm.gui.rest.services.search.graphs;

import com.jecstar.etm.domain.writer.json.JsonBuilder;

import java.util.Objects;

/**
 * Abstract superclass of all <code>Vertex</code> instances.
 */
public abstract class AbstractVertex implements Vertex {

    private final String vertexId;

    AbstractVertex(String vertexId) {
        this.vertexId = vertexId;
    }

    @Override
    public String getVertexId() {
        return this.vertexId;
    }

    @Override
    public Vertex getParent() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractVertex)) return false;
        AbstractVertex that = (AbstractVertex) o;
        return Objects.equals(vertexId, that.vertexId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vertexId);
    }

    @Override
    public String toString() {
        return "id: " + getVertexId();
    }

    @Override
    public void toJson(JsonBuilder builder) {
        builder.field("vertex_id", getVertexId());
        builder.field("type", getType());
        doWriteToJson(builder);
    }

    /**
     * Gives the implementation type of the <code>AbstractVertex</code>.
     *
     * @return The implementation type as string.
     */
    protected abstract String getType();

    /**
     * Add the attributes of the <code>AbstractVertex</code> (without any nested objects) to the buffer as json data.
     *
     * @param builder The <code>JsonBuilder</code> to add the data to.
     */
    protected abstract void doWriteToJson(JsonBuilder builder);

}
