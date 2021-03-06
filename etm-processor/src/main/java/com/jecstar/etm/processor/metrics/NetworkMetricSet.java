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

package com.jecstar.etm.processor.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.codahale.metrics.MetricRegistry.name;

public class NetworkMetricSet implements MetricSet {

    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final NetworkStatistics NETWORK_STATISTICS = new NetworkStatistics();

    public static boolean isCapableOfMonitoring() {
        return NetworkStatistics.canBeMonitored();
    }

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<>();
        List<String> interfaces = NETWORK_STATISTICS.getInterfaceNames();
        for (String interfaceName : interfaces) {
            final String name = WHITESPACE.matcher(interfaceName).replaceAll("-");
            gauges.put(name("network.interfaces", name, "bytes_sent"), (Gauge<Long>) () -> NETWORK_STATISTICS.getBytesSent(interfaceName));
            gauges.put(name("network.interfaces", name, "bytes_received"), (Gauge<Long>) () -> NETWORK_STATISTICS.getBytesReceived(interfaceName));
            gauges.put(name("network.interfaces", name, "packets_sent"), (Gauge<Long>) () -> NETWORK_STATISTICS.getPacketsSent(interfaceName));
            gauges.put(name("network.interfaces", name, "packets_received"), (Gauge<Long>) () -> NETWORK_STATISTICS.getPacketsReceived(interfaceName));
        }
        return gauges;
    }

    private static class NetworkStatistics {

        private static final File proc = new File("/proc/net/dev");

        private final int NAME_IX = 0;
        private final int BYTES_RECEIVED_IX = 1;
        private final int PACKETS_RECEIVED_IX = 2;
        private final int BYTES_SENT_IX = 9;
        private final int PACKETS_SENT_IX = 10;

        private final Long MINUS_ONE = 0L;

        private final Map<String, String[]> interfaceLines = new HashMap<>();
        private long lastUpdated = 0;


        private static boolean canBeMonitored() {
            return proc.exists() && proc.canRead();
        }

        private NetworkStatistics() {
            loadStatistics();
        }

        Long getBytesReceived(String interfaceName) {
            return getData(interfaceName, BYTES_RECEIVED_IX);
        }

        Long getBytesSent(String interfaceName) {
            return getData(interfaceName, BYTES_SENT_IX);
        }

        Long getPacketsReceived(String interfaceName) {
            return getData(interfaceName, PACKETS_RECEIVED_IX);
        }

        Long getPacketsSent(String interfaceName) {
            return getData(interfaceName, PACKETS_SENT_IX);
        }

        private Long getData(String interfaceName, int itemIx) {
            loadStatistics();
            String[] items = interfaceLines.get(interfaceName);
            if (items == null) {
                return MINUS_ONE;
            }
            try {
                return Long.parseLong(items[itemIx]);
            } catch (Exception e) {
                return MINUS_ONE;
            }
        }

        private void loadStatistics() {
            if (System.currentTimeMillis() - this.lastUpdated <= 1000) {
                // Collect only one time per second.
                return;
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(new File("/proc/net/dev")))) {
                // Skip the first 2 lines because it are headers.
                reader.readLine();
                reader.readLine();
                String interfaceStats;
                while ((interfaceStats = reader.readLine()) != null) {
                    String[] stats = WHITESPACE.split(interfaceStats.trim());
                    String interfaceName = stats[NAME_IX];
                    if (interfaceName.endsWith(":")) {
                        interfaceName = interfaceName.substring(0, interfaceName.length() - 1);
                    }
                    this.interfaceLines.put(interfaceName, stats);
                }
            } catch (IOException e) {
            }
            this.lastUpdated = System.currentTimeMillis();
        }

        List<String> getInterfaceNames() {
            return new ArrayList<>(this.interfaceLines.keySet());
        }
    }
}
