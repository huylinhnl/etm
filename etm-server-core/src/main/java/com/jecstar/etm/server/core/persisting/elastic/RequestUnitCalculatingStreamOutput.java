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

package com.jecstar.etm.server.core.persisting.elastic;

import com.jecstar.etm.server.core.domain.configuration.License;
import org.elasticsearch.common.io.stream.StreamOutput;

/**
 * <code>StreamOutput</code> instance that does nothing except counting the number of bytes that are passed through this instance.
 */
public class RequestUnitCalculatingStreamOutput extends StreamOutput {

    private long length = 0;

    @Override
    public void writeByte(byte b) {
        this.length++;
    }

    @Override
    public void writeBytes(byte[] b, int offset, int length) {
        this.length += length;
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    @Override
    public void reset() {
        this.length = 0;
    }

    /**
     * Gives the number of request units that are passed through this instance.
     *
     * @return The request unit amount that has passed through.
     */
    public double getRequestUnits() {
        return (double) this.length / License.BYTES_PER_RU;
    }
}
