/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.commons.event_process.Event;
import org.opensearch.performanceanalyzer.commons.metrics.PerformanceAnalyzerMetrics;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;

public class ClusterManagerThrottlingMetricsProcessorTests {
    private static final String DB_URL = "jdbc:sqlite:";
    private static final Long PENDING_TASKS_NUM = 33L;
    private static final String CMTM_KEY = "cluster_manager_throttling_metrics";
    private static final String SERIALIZED_EVENT =
            "{\"Data_RetryingPendingTasksCount\":0,\"ClusterManager_ThrottledPendingTasksCount\":33}";

    private ClusterManagerThrottlingMetricsEventProcessor cmtmProcessor;
    private long currTimestamp;
    private NavigableMap<Long, ClusterManagerThrottlingMetricsSnapshot> snapMap;
    Connection conn;

    @Before
    public void setup() throws Exception {
        Class.forName("org.sqlite.JDBC");
        System.setProperty("java.io.tmpdir", "/tmp");
        conn = DriverManager.getConnection(DB_URL);
        this.currTimestamp = System.currentTimeMillis();
        this.snapMap = new TreeMap<>();
        this.cmtmProcessor =
                ClusterManagerThrottlingMetricsEventProcessor
                        .buildClusterManagerThrottlingMetricEventsProcessor(
                                currTimestamp, conn, snapMap);
    }

    @Test
    public void testHandleEvent() {
        Event testEvent = buildTestCMTMEvent();

        cmtmProcessor.initializeProcessing(currTimestamp, System.currentTimeMillis());

        assertTrue(cmtmProcessor.shouldProcessEvent(testEvent));

        cmtmProcessor.processEvent(testEvent);
        cmtmProcessor.finalizeProcessing();

        ClusterManagerThrottlingMetricsSnapshot snap = snapMap.get(currTimestamp);
        Result<Record> result = snap.fetchAll();
        assertEquals(1, result.size());
        Assert.assertEquals(
                PENDING_TASKS_NUM,
                result.get(0)
                        .get(
                                AllMetrics.ClusterManagerThrottlingValue
                                        .CLUSTER_MANAGER_THROTTLED_PENDING_TASK_COUNT
                                        .toString()));
    }

    private Event buildTestCMTMEvent() {
        StringBuilder val = new StringBuilder();
        val.append(PerformanceAnalyzerMetrics.getJsonCurrentMilliSeconds())
                .append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);

        val.append(SERIALIZED_EVENT).append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);

        return new Event(CMTM_KEY, val.toString(), System.currentTimeMillis());
    }
}
