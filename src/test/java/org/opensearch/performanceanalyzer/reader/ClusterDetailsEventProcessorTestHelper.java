/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 * Copyright 2019-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.opensearch.performanceanalyzer.reader;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.opensearch.performanceanalyzer.config.overrides.ConfigOverrides;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metrics.PerformanceAnalyzerMetrics;
import org.opensearch.performanceanalyzer.reader_writer_shared.Event;
import org.opensearch.performanceanalyzer.util.JsonConverter;

public class ClusterDetailsEventProcessorTestHelper extends AbstractReaderTests {

    private static final String SEPARATOR = System.getProperty("line.separator");
    List<String> nodeDetails;

    public ClusterDetailsEventProcessorTestHelper() throws SQLException, ClassNotFoundException {
        super();
        nodeDetails = new ArrayList<>();
    }

    public void addNodeDetails(String nodeId, String address, boolean isMasterNode) {
        nodeDetails.add(createNodeDetailsMetrics(nodeId, address, isMasterNode));
    }

    public void addNodeDetails(
            String nodeId, String address, AllMetrics.NodeRole nodeRole, boolean isMasterNode) {
        nodeDetails.add(createNodeDetailsMetrics(nodeId, address, nodeRole, isMasterNode));
    }

    public static ClusterDetailsEventProcessor.NodeDetails newNodeDetails(
            final String nodeId, final String address, final boolean isMasterNode) {
        return createNodeDetails(nodeId, address, isMasterNode);
    }

    public ClusterDetailsEventProcessor generateClusterDetailsEvent() {
        if (nodeDetails.isEmpty()) {
            return new ClusterDetailsEventProcessor();
        }
        Event testEvent = generateTestEvent();
        ClusterDetailsEventProcessor clusterDetailsEventProcessor =
                new ClusterDetailsEventProcessor();
        clusterDetailsEventProcessor.processEvent(testEvent);
        return clusterDetailsEventProcessor;
    }

    public Event generateTestEvent() {
        return generateTestEventWithOverrides(new ConfigOverrides());
    }

    public Event generateTestEventWithOverrides(ConfigOverrides overrides) {
        StringBuilder stringBuilder =
                new StringBuilder().append(PerformanceAnalyzerMetrics.getJsonCurrentMilliSeconds());
        stringBuilder.append(SEPARATOR);
        stringBuilder.append(JsonConverter.writeValueAsString(overrides));
        stringBuilder.append(SEPARATOR);
        stringBuilder.append(System.currentTimeMillis());
        nodeDetails.stream()
                .forEach(
                        node -> {
                            stringBuilder.append(SEPARATOR).append(node);
                        });
        return new Event("", stringBuilder.toString(), 0);
    }
}
