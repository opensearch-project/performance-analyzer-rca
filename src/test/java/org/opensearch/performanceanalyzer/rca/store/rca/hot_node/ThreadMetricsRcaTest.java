/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.hot_node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.opensearch.performanceanalyzer.metrics.AllMetrics.CommonDimension.Constants.THREAD_NAME;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.MetricTestHelper;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Thread_Blocked_Time;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Thread_Waited_Time;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.reader.ShardRequestMetricsSnapshot;

public class ThreadMetricsRcaTest {

    @Mock private Thread_Blocked_Time mockThreadBlockedTime;
    @Mock private Thread_Waited_Time mockThreadWaitedTime;

    private static final int PERIOD = 5;
    private ThreadMetricsRca rca;
    private MetricTestHelper metricTestHelper;
    private final List<String> threadBlockedTimeTableColumns =
            Arrays.asList(
                    ShardRequestMetricsSnapshot.Fields.SHARD_ID.toString(),
                    ShardRequestMetricsSnapshot.Fields.INDEX_NAME.toString(),
                    ShardRequestMetricsSnapshot.Fields.OPERATION.toString(),
                    ShardRequestMetricsSnapshot.Fields.SHARD_ROLE.toString(),
                    THREAD_NAME,
                    MetricsDB.SUM,
                    MetricsDB.AVG,
                    MetricsDB.MIN,
                    MetricsDB.MAX);

    private final List<String> threadWaitedTimeTableColumns =
            Arrays.asList(
                    ShardRequestMetricsSnapshot.Fields.SHARD_ID.toString(),
                    ShardRequestMetricsSnapshot.Fields.INDEX_NAME.toString(),
                    ShardRequestMetricsSnapshot.Fields.OPERATION.toString(),
                    ShardRequestMetricsSnapshot.Fields.SHARD_ROLE.toString(),
                    THREAD_NAME,
                    MetricsDB.SUM,
                    MetricsDB.AVG,
                    MetricsDB.MIN,
                    MetricsDB.MAX);

    @Before
    public void setup() throws Exception {
        initMocks(this);
        this.metricTestHelper = new MetricTestHelper(PERIOD);
        this.rca = new ThreadMetricsRca(mockThreadBlockedTime, mockThreadWaitedTime, PERIOD);
    }

    @Test
    public void testOperateWithTransportThread() {
        setupMockThreadMetric(
                mockThreadBlockedTime, threadBlockedTimeTableColumns, "10", "transport");
        setupMockThreadMetric(mockThreadWaitedTime, threadWaitedTimeTableColumns, "0", "transport");
        ResourceFlowUnit<HotNodeSummary> rfu = rca.operate();
        assertTrue(rfu.isEmpty());
        assertEquals(rca.threadAnalyses.size(), 1);
        assertEquals(
                rca.threadAnalyses.get(0).getBlockedTimeWindow().getCountExceedingThreshold(5), 1);
    }

    @Test
    public void testOperateWithNonTransportThread() {
        setupMockThreadMetric(mockThreadBlockedTime, threadBlockedTimeTableColumns, "10", "search");
        setupMockThreadMetric(mockThreadWaitedTime, threadWaitedTimeTableColumns, "0", "search");
        ResourceFlowUnit<HotNodeSummary> rfu = rca.operate();
        assertTrue(rfu.isEmpty());
        assertEquals(rca.threadAnalyses.size(), 1);
        assertEquals(
                rca.threadAnalyses.get(0).getBlockedTimeWindow().getCountExceedingThreshold(5), 0);
    }

    private void setupMockThreadMetric(
            Metric metric, List<String> columns, String val, String operation) {

        List<String> data =
                Arrays.asList(
                        "shardId",
                        "indexName",
                        operation,
                        "shardRole",
                        "transport",
                        val,
                        val,
                        val,
                        val);
        when(metric.getFlowUnits())
                .thenReturn(
                        Collections.singletonList(
                                new MetricFlowUnit(
                                        0, metricTestHelper.createTestResult(columns, data))));
    }
}
