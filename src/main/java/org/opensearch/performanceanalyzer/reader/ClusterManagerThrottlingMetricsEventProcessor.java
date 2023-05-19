/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.BatchBindStep;
import org.opensearch.performanceanalyzer.commons.metrics.PerformanceAnalyzerMetrics;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.reader_writer_shared.Event;

public class ClusterManagerThrottlingMetricsEventProcessor implements EventProcessor {
    private static final Logger LOG =
            LogManager.getLogger(ClusterManagerThrottlingMetricsEventProcessor.class);
    private final ClusterManagerThrottlingMetricsSnapshot clusterManagerThrottlingMetricsSnapshot;
    private BatchBindStep handle;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<HashMap<String, String>> TYPE_REF =
            new TypeReference<HashMap<String, String>>() {};

    private ClusterManagerThrottlingMetricsEventProcessor(
            ClusterManagerThrottlingMetricsSnapshot snapshot) {
        this.clusterManagerThrottlingMetricsSnapshot = snapshot;
    }

    static ClusterManagerThrottlingMetricsEventProcessor
            buildClusterManagerThrottlingMetricEventsProcessor(
                    long currWindowStartTime,
                    Connection conn,
                    NavigableMap<Long, ClusterManagerThrottlingMetricsSnapshot>
                            clusterManagerThroEventMetricsMap) {
        ClusterManagerThrottlingMetricsSnapshot clusterManagerThrottlingSnapshot =
                clusterManagerThroEventMetricsMap.get(currWindowStartTime);
        if (clusterManagerThrottlingSnapshot == null) {
            clusterManagerThrottlingSnapshot =
                    new ClusterManagerThrottlingMetricsSnapshot(conn, currWindowStartTime);
            clusterManagerThroEventMetricsMap.put(
                    currWindowStartTime, clusterManagerThrottlingSnapshot);
        }
        return new ClusterManagerThrottlingMetricsEventProcessor(clusterManagerThrottlingSnapshot);
    }

    @Override
    public void initializeProcessing(long startTime, long endTime) {
        this.handle = clusterManagerThrottlingMetricsSnapshot.startBatchPut();
    }

    @Override
    public void finalizeProcessing() {
        if (handle.size() > 0) {
            handle.execute();
        }
        LOG.debug(
                "Final CLusterManager Throttling metrics {}",
                clusterManagerThrottlingMetricsSnapshot.fetchAll());
    }

    /**
     * Sample event: ^cluster_manager_throttling_metrics {"current_time":1602617137529}
     * {"Data_RetryingPendingTasksCount":0,"ClusterManager_ThrottledPendingTasksCount":0}$
     *
     * @param event event
     */
    @Override
    public void processEvent(Event event) {
        String[] lines = event.value.split(System.lineSeparator());
        for (String line : lines) {
            Map<String, String> clusterManagerThrottlingMap = extractEntryData(line);
            if (!clusterManagerThrottlingMap.containsKey(
                    PerformanceAnalyzerMetrics.METRIC_CURRENT_TIME)) {
                try {
                    handle.bind(
                            Long.parseLong(
                                    clusterManagerThrottlingMap.get(
                                            AllMetrics.ClusterManagerThrottlingValue
                                                    .DATA_RETRYING_TASK_COUNT
                                                    .toString())),
                            Long.parseLong(
                                    clusterManagerThrottlingMap.get(
                                            AllMetrics.ClusterManagerThrottlingValue
                                                    .CLUSTER_MANAGER_THROTTLED_PENDING_TASK_COUNT
                                                    .toString())));
                } catch (Exception ex) {
                    LOG.error("Fail to get cluster_manager throttling metrics ", ex);
                }
            }
        }
    }

    @Override
    public boolean shouldProcessEvent(Event event) {
        return event.key.contains(PerformanceAnalyzerMetrics.sClusterManagerThrottledTasksPath);
    }

    @Override
    public void commitBatchIfRequired() {
        if (handle.size() > BATCH_LIMIT) {
            handle.execute();
            handle = clusterManagerThrottlingMetricsSnapshot.startBatchPut();
        }
    }

    static Map<String, String> extractEntryData(String line) {
        try {
            return MAPPER.readValue(line, TYPE_REF);
        } catch (IOException ioe) {
            LOG.error("Error occurred while parsing tmp file", ioe);
        }
        return new HashMap<>();
    }
}
