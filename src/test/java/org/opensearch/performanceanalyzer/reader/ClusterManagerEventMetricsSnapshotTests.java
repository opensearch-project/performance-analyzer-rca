/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import org.jooq.BatchBindStep;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;

public class ClusterManagerEventMetricsSnapshotTests {

    private static final String DB_URL = "jdbc:sqlite:";
    private Connection conn;

    @Before
    public void setup() throws Exception {
        Class.forName("org.sqlite.JDBC");
        System.setProperty("java.io.tmpdir", "/tmp");
        conn = DriverManager.getConnection(DB_URL);
    }

    @Test
    public void testStartEventOnly() {
        ClusterManagerEventMetricsSnapshot clusterManagerEventMetricsSnapshot =
                new ClusterManagerEventMetricsSnapshot(conn, 1535065195000L);
        BatchBindStep handle = clusterManagerEventMetricsSnapshot.startBatchPut();

        handle.bind("111", "1", "urgent", "create-index", "metadata", 12, 1535065195001L, null);
        handle.execute();
        Result<Record> rt = clusterManagerEventMetricsSnapshot.fetchQueueAndRunTime();

        assertEquals(1, rt.size());
        assertEquals(
                4999L,
                ((BigDecimal)
                                (rt.get(0)
                                        .get(
                                                "sum_"
                                                        + AllMetrics.ClusterManagerMetricDimensions
                                                                .CLUSTER_MANAGER_TASK_RUN_TIME
                                                                .toString())))
                        .longValue());
        assertEquals(
                "urgent",
                rt.get(0)
                        .get(
                                AllMetrics.ClusterManagerMetricDimensions
                                        .CLUSTER_MANAGER_TASK_PRIORITY
                                        .toString()));
        assertEquals(
                "create-index",
                rt.get(0)
                        .get(
                                AllMetrics.ClusterManagerMetricDimensions.CLUSTER_MANAGER_TASK_TYPE
                                        .toString()));
        assertEquals(
                "metadata",
                rt.get(0)
                        .get(
                                AllMetrics.ClusterManagerMetricDimensions
                                        .CLUSTER_MANAGER_TASK_METADATA
                                        .toString()));
        assertEquals(
                12L,
                ((BigDecimal)
                                (rt.get(0)
                                        .get(
                                                "sum_"
                                                        + AllMetrics.ClusterManagerMetricDimensions
                                                                .CLUSTER_MANAGER_TASK_QUEUE_TIME
                                                                .toString())))
                        .longValue());
    }

    @Test
    public void testStartAndEndEvents() {
        ClusterManagerEventMetricsSnapshot clusterManagerEventMetricsSnapshot =
                new ClusterManagerEventMetricsSnapshot(conn, 1535065195000L);
        BatchBindStep handle = clusterManagerEventMetricsSnapshot.startBatchPut();

        handle.bind("111", "1", "urgent", "create-index", "metadata", 12, 1535065195001L, null);
        handle.bind("111", "1", null, null, null, 12, null, 1535065195005L);
        handle.execute();
        Result<Record> rt = clusterManagerEventMetricsSnapshot.fetchQueueAndRunTime();

        assertEquals(1, rt.size());
        assertEquals(
                4L,
                ((BigDecimal)
                                (rt.get(0)
                                        .get(
                                                "sum_"
                                                        + AllMetrics.ClusterManagerMetricDimensions
                                                                .CLUSTER_MANAGER_TASK_RUN_TIME
                                                                .toString())))
                        .longValue());
        assertEquals(
                "urgent",
                rt.get(0)
                        .get(
                                AllMetrics.ClusterManagerMetricDimensions
                                        .CLUSTER_MANAGER_TASK_PRIORITY
                                        .toString()));
        assertEquals(
                "create-index",
                rt.get(0)
                        .get(
                                AllMetrics.ClusterManagerMetricDimensions.CLUSTER_MANAGER_TASK_TYPE
                                        .toString()));
        assertEquals(
                "metadata",
                rt.get(0)
                        .get(
                                AllMetrics.ClusterManagerMetricDimensions
                                        .CLUSTER_MANAGER_TASK_METADATA
                                        .toString()));
        assertEquals(
                12L,
                ((BigDecimal)
                                (rt.get(0)
                                        .get(
                                                "sum_"
                                                        + AllMetrics.ClusterManagerMetricDimensions
                                                                .CLUSTER_MANAGER_TASK_QUEUE_TIME
                                                                .toString())))
                        .longValue());
    }

    @Test
    public void testMultipleInsertOrderStartAndEndEvents() {
        ClusterManagerEventMetricsSnapshot clusterManagerEventMetricsSnapshot =
                new ClusterManagerEventMetricsSnapshot(conn, 1535065195000L);
        BatchBindStep handle = clusterManagerEventMetricsSnapshot.startBatchPut();

        handle.bind("111", "1", "urgent", "create-index", "metadata", 12, 1535065195001L, null);
        handle.bind("111", "1", null, null, null, 12, null, 1535065195005L);
        handle.bind("111", "2", "high", "remapping", "metadata2", 2, 1535065195007L, null);
        handle.execute();

        Result<Record> rt = clusterManagerEventMetricsSnapshot.fetchQueueAndRunTime();

        assertEquals(2, rt.size());
        assertEquals(
                4L,
                ((BigDecimal)
                                (rt.get(0)
                                        .get(
                                                "sum_"
                                                        + AllMetrics.ClusterManagerMetricDimensions
                                                                .CLUSTER_MANAGER_TASK_RUN_TIME
                                                                .toString())))
                        .longValue());
        assertEquals(
                "urgent",
                rt.get(0)
                        .get(
                                AllMetrics.ClusterManagerMetricDimensions
                                        .CLUSTER_MANAGER_TASK_PRIORITY
                                        .toString()));
        assertEquals(
                "create-index",
                rt.get(0)
                        .get(
                                AllMetrics.ClusterManagerMetricDimensions.CLUSTER_MANAGER_TASK_TYPE
                                        .toString()));
        assertEquals(
                "metadata",
                rt.get(0)
                        .get(
                                AllMetrics.ClusterManagerMetricDimensions
                                        .CLUSTER_MANAGER_TASK_METADATA
                                        .toString()));
        assertEquals(
                12L,
                ((BigDecimal)
                                (rt.get(0)
                                        .get(
                                                "sum_"
                                                        + AllMetrics.ClusterManagerMetricDimensions
                                                                .CLUSTER_MANAGER_TASK_QUEUE_TIME
                                                                .toString())))
                        .longValue());

        assertEquals(
                4993L,
                ((BigDecimal)
                                (rt.get(1)
                                        .get(
                                                "sum_"
                                                        + AllMetrics.ClusterManagerMetricDimensions
                                                                .CLUSTER_MANAGER_TASK_RUN_TIME
                                                                .toString())))
                        .longValue());
        assertEquals(
                "high",
                rt.get(1)
                        .get(
                                AllMetrics.ClusterManagerMetricDimensions
                                        .CLUSTER_MANAGER_TASK_PRIORITY
                                        .toString()));
        assertEquals(
                "remapping",
                rt.get(1)
                        .get(
                                AllMetrics.ClusterManagerMetricDimensions.CLUSTER_MANAGER_TASK_TYPE
                                        .toString()));
        assertEquals(
                "metadata2",
                rt.get(1)
                        .get(
                                AllMetrics.ClusterManagerMetricDimensions
                                        .CLUSTER_MANAGER_TASK_METADATA
                                        .toString()));
        assertEquals(
                2L,
                ((BigDecimal)
                                (rt.get(1)
                                        .get(
                                                "sum_"
                                                        + AllMetrics.ClusterManagerMetricDimensions
                                                                .CLUSTER_MANAGER_TASK_QUEUE_TIME
                                                                .toString())))
                        .longValue());
    }

    @Test
    public void testRollOver() {
        ClusterManagerEventMetricsSnapshot clusterManagerEventMetricsSnapshotPre =
                new ClusterManagerEventMetricsSnapshot(conn, 1535065195000L);
        BatchBindStep handle = clusterManagerEventMetricsSnapshotPre.startBatchPut();

        handle.bind("111", "1", "urgent", "create-index", "metadata", 12, 1535065195001L, null);
        handle.execute();

        ClusterManagerEventMetricsSnapshot clusterManagerEventMetricsSnapshotCurrent =
                new ClusterManagerEventMetricsSnapshot(conn, 1535065200000L);
        Result<Record> rt = clusterManagerEventMetricsSnapshotCurrent.fetchAll();
        assertEquals(0, rt.size());

        clusterManagerEventMetricsSnapshotCurrent.rolloverInflightRequests(
                clusterManagerEventMetricsSnapshotPre);

        Result<Record> rt2 = clusterManagerEventMetricsSnapshotCurrent.fetchAll();
        assertEquals(1, rt2.size());
    }

    @Test
    public void testNotRollOverExpired() {
        ClusterManagerEventMetricsSnapshot clusterManagerEventMetricsSnapshotPre =
                new ClusterManagerEventMetricsSnapshot(conn, 1535065195000L);
        BatchBindStep handle = clusterManagerEventMetricsSnapshotPre.startBatchPut();

        handle.bind("111", "1", "urgent", "create-index", "metadata", 12, 1435065195001L, null);
        handle.execute();

        ClusterManagerEventMetricsSnapshot clusterManagerEventMetricsSnapshotCurrent =
                new ClusterManagerEventMetricsSnapshot(conn, 1535065200000L);
        Result<Record> rt = clusterManagerEventMetricsSnapshotCurrent.fetchAll();
        assertEquals(0, rt.size());

        clusterManagerEventMetricsSnapshotCurrent.rolloverInflightRequests(
                clusterManagerEventMetricsSnapshotPre);

        Result<Record> rt2 = clusterManagerEventMetricsSnapshotCurrent.fetchAll();
        assertEquals(0, rt2.size());
    }
}
