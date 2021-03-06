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
import org.opensearch.performanceanalyzer.metrics.AllMetrics;

public class MasterEventMetricsSnapshotTests {

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
        MasterEventMetricsSnapshot masterEventMetricsSnapshot =
                new MasterEventMetricsSnapshot(conn, 1535065195000L);
        BatchBindStep handle = masterEventMetricsSnapshot.startBatchPut();

        handle.bind("111", "1", "urgent", "create-index", "metadata", 12, 1535065195001L, null);
        handle.execute();
        Result<Record> rt = masterEventMetricsSnapshot.fetchQueueAndRunTime();

        assertEquals(1, rt.size());
        assertEquals(
                4999L,
                ((BigDecimal)
                                (rt.get(0)
                                        .get(
                                                "sum_"
                                                        + AllMetrics.MasterMetricDimensions
                                                                .MASTER_TASK_RUN_TIME
                                                                .toString())))
                        .longValue());
        assertEquals(
                "urgent",
                rt.get(0).get(AllMetrics.MasterMetricDimensions.MASTER_TASK_PRIORITY.toString()));
        assertEquals(
                "create-index",
                rt.get(0).get(AllMetrics.MasterMetricDimensions.MASTER_TASK_TYPE.toString()));
        assertEquals(
                "metadata",
                rt.get(0).get(AllMetrics.MasterMetricDimensions.MASTER_TASK_METADATA.toString()));
        assertEquals(
                12L,
                ((BigDecimal)
                                (rt.get(0)
                                        .get(
                                                "sum_"
                                                        + AllMetrics.MasterMetricDimensions
                                                                .MASTER_TASK_QUEUE_TIME
                                                                .toString())))
                        .longValue());
    }

    @Test
    public void testStartAndEndEvents() {
        MasterEventMetricsSnapshot masterEventMetricsSnapshot =
                new MasterEventMetricsSnapshot(conn, 1535065195000L);
        BatchBindStep handle = masterEventMetricsSnapshot.startBatchPut();

        handle.bind("111", "1", "urgent", "create-index", "metadata", 12, 1535065195001L, null);
        handle.bind("111", "1", null, null, null, 12, null, 1535065195005L);
        handle.execute();
        Result<Record> rt = masterEventMetricsSnapshot.fetchQueueAndRunTime();

        assertEquals(1, rt.size());
        assertEquals(
                4L,
                ((BigDecimal)
                                (rt.get(0)
                                        .get(
                                                "sum_"
                                                        + AllMetrics.MasterMetricDimensions
                                                                .MASTER_TASK_RUN_TIME
                                                                .toString())))
                        .longValue());
        assertEquals(
                "urgent",
                rt.get(0).get(AllMetrics.MasterMetricDimensions.MASTER_TASK_PRIORITY.toString()));
        assertEquals(
                "create-index",
                rt.get(0).get(AllMetrics.MasterMetricDimensions.MASTER_TASK_TYPE.toString()));
        assertEquals(
                "metadata",
                rt.get(0).get(AllMetrics.MasterMetricDimensions.MASTER_TASK_METADATA.toString()));
        assertEquals(
                12L,
                ((BigDecimal)
                                (rt.get(0)
                                        .get(
                                                "sum_"
                                                        + AllMetrics.MasterMetricDimensions
                                                                .MASTER_TASK_QUEUE_TIME
                                                                .toString())))
                        .longValue());
    }

    @Test
    public void testMultipleInsertOrderStartAndEndEvents() {
        MasterEventMetricsSnapshot masterEventMetricsSnapshot =
                new MasterEventMetricsSnapshot(conn, 1535065195000L);
        BatchBindStep handle = masterEventMetricsSnapshot.startBatchPut();

        handle.bind("111", "1", "urgent", "create-index", "metadata", 12, 1535065195001L, null);
        handle.bind("111", "1", null, null, null, 12, null, 1535065195005L);
        handle.bind("111", "2", "high", "remapping", "metadata2", 2, 1535065195007L, null);
        handle.execute();

        Result<Record> rt = masterEventMetricsSnapshot.fetchQueueAndRunTime();

        assertEquals(2, rt.size());
        assertEquals(
                4L,
                ((BigDecimal)
                                (rt.get(0)
                                        .get(
                                                "sum_"
                                                        + AllMetrics.MasterMetricDimensions
                                                                .MASTER_TASK_RUN_TIME
                                                                .toString())))
                        .longValue());
        assertEquals(
                "urgent",
                rt.get(0).get(AllMetrics.MasterMetricDimensions.MASTER_TASK_PRIORITY.toString()));
        assertEquals(
                "create-index",
                rt.get(0).get(AllMetrics.MasterMetricDimensions.MASTER_TASK_TYPE.toString()));
        assertEquals(
                "metadata",
                rt.get(0).get(AllMetrics.MasterMetricDimensions.MASTER_TASK_METADATA.toString()));
        assertEquals(
                12L,
                ((BigDecimal)
                                (rt.get(0)
                                        .get(
                                                "sum_"
                                                        + AllMetrics.MasterMetricDimensions
                                                                .MASTER_TASK_QUEUE_TIME
                                                                .toString())))
                        .longValue());

        assertEquals(
                4993L,
                ((BigDecimal)
                                (rt.get(1)
                                        .get(
                                                "sum_"
                                                        + AllMetrics.MasterMetricDimensions
                                                                .MASTER_TASK_RUN_TIME
                                                                .toString())))
                        .longValue());
        assertEquals(
                "high",
                rt.get(1).get(AllMetrics.MasterMetricDimensions.MASTER_TASK_PRIORITY.toString()));
        assertEquals(
                "remapping",
                rt.get(1).get(AllMetrics.MasterMetricDimensions.MASTER_TASK_TYPE.toString()));
        assertEquals(
                "metadata2",
                rt.get(1).get(AllMetrics.MasterMetricDimensions.MASTER_TASK_METADATA.toString()));
        assertEquals(
                2L,
                ((BigDecimal)
                                (rt.get(1)
                                        .get(
                                                "sum_"
                                                        + AllMetrics.MasterMetricDimensions
                                                                .MASTER_TASK_QUEUE_TIME
                                                                .toString())))
                        .longValue());
    }

    @Test
    public void testRollOver() {
        MasterEventMetricsSnapshot masterEventMetricsSnapshotPre =
                new MasterEventMetricsSnapshot(conn, 1535065195000L);
        BatchBindStep handle = masterEventMetricsSnapshotPre.startBatchPut();

        handle.bind("111", "1", "urgent", "create-index", "metadata", 12, 1535065195001L, null);
        handle.execute();

        MasterEventMetricsSnapshot masterEventMetricsSnapshotCurrent =
                new MasterEventMetricsSnapshot(conn, 1535065200000L);
        Result<Record> rt = masterEventMetricsSnapshotCurrent.fetchAll();
        assertEquals(0, rt.size());

        masterEventMetricsSnapshotCurrent.rolloverInflightRequests(masterEventMetricsSnapshotPre);

        Result<Record> rt2 = masterEventMetricsSnapshotCurrent.fetchAll();
        assertEquals(1, rt2.size());
    }

    @Test
    public void testNotRollOverExpired() {
        MasterEventMetricsSnapshot masterEventMetricsSnapshotPre =
                new MasterEventMetricsSnapshot(conn, 1535065195000L);
        BatchBindStep handle = masterEventMetricsSnapshotPre.startBatchPut();

        handle.bind("111", "1", "urgent", "create-index", "metadata", 12, 1435065195001L, null);
        handle.execute();

        MasterEventMetricsSnapshot masterEventMetricsSnapshotCurrent =
                new MasterEventMetricsSnapshot(conn, 1535065200000L);
        Result<Record> rt = masterEventMetricsSnapshotCurrent.fetchAll();
        assertEquals(0, rt.size());

        masterEventMetricsSnapshotCurrent.rolloverInflightRequests(masterEventMetricsSnapshotPre);

        Result<Record> rt2 = masterEventMetricsSnapshotCurrent.fetchAll();
        assertEquals(0, rt2.size());
    }
}
