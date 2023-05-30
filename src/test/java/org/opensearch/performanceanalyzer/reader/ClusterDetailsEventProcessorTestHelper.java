/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.opensearch.performanceanalyzer.commons.config.overrides.ConfigOverrides;
import org.opensearch.performanceanalyzer.commons.event_process.Event;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.commons.metrics.PerformanceAnalyzerMetrics;
import org.opensearch.performanceanalyzer.commons.util.JsonConverter;

public class ClusterDetailsEventProcessorTestHelper extends AbstractReaderTests {

    private static final String SEPARATOR = System.getProperty("line.separator");
    List<String> nodeDetails;

    public ClusterDetailsEventProcessorTestHelper() throws SQLException, ClassNotFoundException {
        super();
        nodeDetails = new ArrayList<>();
    }

    public void addNodeDetails(String nodeId, String address, boolean isClusterManagerNode) {
        nodeDetails.add(createNodeDetailsMetrics(nodeId, address, isClusterManagerNode));
    }

    public void addNodeDetails(
            String nodeId,
            String address,
            AllMetrics.NodeRole nodeRole,
            boolean isClusterManagerNode) {
        nodeDetails.add(createNodeDetailsMetrics(nodeId, address, nodeRole, isClusterManagerNode));
    }

    public static ClusterDetailsEventProcessor.NodeDetails newNodeDetails(
            final String nodeId, final String address, final boolean isClusterManagerNode) {
        return createNodeDetails(nodeId, address, isClusterManagerNode);
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
