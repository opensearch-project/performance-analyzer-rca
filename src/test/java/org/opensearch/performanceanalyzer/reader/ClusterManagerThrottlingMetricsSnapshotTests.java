/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import org.jooq.BatchBindStep;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;

public class ClusterManagerThrottlingMetricsSnapshotTests {
    private static final String DB_URL = "jdbc:sqlite:";
    private Connection conn;

    @Before
    public void setup() throws Exception {
        Class.forName("org.sqlite.JDBC");
        System.setProperty("java.io.tmpdir", "/tmp");
        conn = DriverManager.getConnection(DB_URL);
    }

    @Test
    public void testPutMetrics() {
        ClusterManagerThrottlingMetricsSnapshot clusterManagerThrottlingMetricsSnapshot =
                new ClusterManagerThrottlingMetricsSnapshot(conn, 1535065195000L);
        BatchBindStep handle = clusterManagerThrottlingMetricsSnapshot.startBatchPut();

        handle.bind(1, 5);
        handle.execute();
        Result<Record> rt = clusterManagerThrottlingMetricsSnapshot.fetchAggregatedMetrics();

        assertEquals(1, rt.size());
        Double total_throttled =
                Double.parseDouble(
                        rt.get(0)
                                .get(
                                        "max_"
                                                + AllMetrics.ClusterManagerThrottlingValue
                                                        .CLUSTER_MANAGER_THROTTLED_PENDING_TASK_COUNT
                                                        .toString())
                                .toString());
        assertEquals(5.0, total_throttled.doubleValue(), 0);

        Double retrying_task =
                Double.parseDouble(
                        rt.get(0)
                                .get(
                                        "max_"
                                                + AllMetrics.ClusterManagerThrottlingValue
                                                        .DATA_RETRYING_TASK_COUNT
                                                        .toString())
                                .toString());
        assertEquals(1.0, retrying_task.doubleValue(), 0);
    }
}
