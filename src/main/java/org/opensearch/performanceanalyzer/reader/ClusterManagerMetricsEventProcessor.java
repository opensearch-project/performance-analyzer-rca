/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;


import java.io.File;
import java.sql.Connection;
import java.util.Map;
import java.util.NavigableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.BatchBindStep;
import org.opensearch.performanceanalyzer.commons.metrics.PerformanceAnalyzerMetrics;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.reader_writer_shared.Event;

public class ClusterManagerMetricsEventProcessor implements EventProcessor {
    private static final Logger LOG =
            LogManager.getLogger(ClusterManagerMetricsEventProcessor.class);
    private ClusterManagerEventMetricsSnapshot clusterManagerSnap;
    private BatchBindStep handle;
    private long startTime;
    private long endTime;

    private ClusterManagerMetricsEventProcessor(
            ClusterManagerEventMetricsSnapshot clusterManagerSnap) {
        this.clusterManagerSnap = clusterManagerSnap;
    }

    static ClusterManagerMetricsEventProcessor buildClusterManagerMetricEventsProcessor(
            long currWindowStartTime,
            Connection conn,
            NavigableMap<Long, ClusterManagerEventMetricsSnapshot> clusterManagerEventMetricsMap) {
        ClusterManagerEventMetricsSnapshot clusterManagerSnap =
                clusterManagerEventMetricsMap.get(currWindowStartTime);
        if (clusterManagerSnap == null) {
            clusterManagerSnap = new ClusterManagerEventMetricsSnapshot(conn, currWindowStartTime);
            Map.Entry<Long, ClusterManagerEventMetricsSnapshot> entry =
                    clusterManagerEventMetricsMap.lastEntry();
            if (entry != null) {
                clusterManagerSnap.rolloverInflightRequests(entry.getValue());
            }
            clusterManagerEventMetricsMap.put(currWindowStartTime, clusterManagerSnap);
        }
        return new ClusterManagerMetricsEventProcessor(clusterManagerSnap);
    }

    @Override
    public void initializeProcessing(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.handle = clusterManagerSnap.startBatchPut();
    }

    @Override
    public void finalizeProcessing() {
        if (handle.size() > 0) {
            handle.execute();
        }
        LOG.debug("Final clusterManagerEvents request metrics {}", clusterManagerSnap.fetchAll());
    }

    @Override
    public void processEvent(Event event) {
        String[] keyElements =
                event.key.split(File.separatorChar == '\\' ? "\\\\" : File.separator);
        String threadId = keyElements[1];
        String insertOrder = keyElements[3];
        String startOrFinish = keyElements[4];
        if (startOrFinish.equals(PerformanceAnalyzerMetrics.START_FILE_NAME)) {
            emitStartClusterManagerEventMetric(event, insertOrder, threadId);
        } else if (startOrFinish.equals(PerformanceAnalyzerMetrics.FINISH_FILE_NAME)) {
            emitEndClusterManagerEventMetric(event, insertOrder, threadId);
        }
    }

    @Override
    public boolean shouldProcessEvent(Event event) {
        return event.key.contains(PerformanceAnalyzerMetrics.sClusterManagerTaskPath);
    }

    @Override
    public void commitBatchIfRequired() {
        if (handle.size() > BATCH_LIMIT) {
            handle.execute();
            handle = clusterManagerSnap.startBatchPut();
        }
    }

    // threads/7462/cluster_manager_task/245/start
    // current_time:1566413947489
    // ClusterManagerTaskPriority:URGENT
    // StartTime:1566413946989
    // ClusterManagerTaskType:delete-index
    // ClusterManagerTaskMetadata: [[nyc_taxis/f1i57IF8RCeI9nsKiLRMOg]]
    // ClusterManagerTaskQueueTime:11$
    private void emitStartClusterManagerEventMetric(
            Event entry, String insertOrder, String threadId) {

        Map<String, String> keyValueMap = ReaderMetricsProcessor.extractEntryData(entry.value);
        String priority =
                keyValueMap.get(
                        AllMetrics.ClusterManagerMetricDimensions.CLUSTER_MANAGER_TASK_PRIORITY
                                .toString());
        long st = Long.parseLong(keyValueMap.get(AllMetrics.CommonMetric.START_TIME.toString()));
        String taskType =
                keyValueMap.get(
                        AllMetrics.ClusterManagerMetricDimensions.CLUSTER_MANAGER_TASK_TYPE
                                .toString());
        String taskMetadata =
                keyValueMap.get(
                        AllMetrics.ClusterManagerMetricDimensions.CLUSTER_MANAGER_TASK_METADATA
                                .toString());
        long queueTime =
                Long.parseLong(
                        keyValueMap.get(
                                AllMetrics.ClusterManagerMetricDimensions
                                        .CLUSTER_MANAGER_TASK_QUEUE_TIME
                                        .toString()));

        handle.bind(threadId, insertOrder, priority, taskType, taskMetadata, queueTime, st, null);
    }

    // An example cluster_manager_task finish
    // threads/7462/cluster_manager_task/245/finish
    // current_time:1566413959491
    // FinishTime:1566413958991
    private void emitEndClusterManagerEventMetric(
            Event entry, String insertOrder, String threadId) {
        Map<String, String> keyValueMap = ReaderMetricsProcessor.extractEntryData(entry.value);
        long finishTime =
                Long.parseLong(keyValueMap.get(AllMetrics.CommonMetric.FINISH_TIME.toString()));
        handle.bind(threadId, insertOrder, null, null, null, null, null, finishTime);
    }
}
