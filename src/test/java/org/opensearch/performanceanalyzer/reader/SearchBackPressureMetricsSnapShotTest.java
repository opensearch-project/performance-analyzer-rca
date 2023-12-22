/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import org.jooq.BatchBindStep;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;

public class SearchBackPressureMetricsSnapShotTest {
    private static final String DB_URL = "jdbc:sqlite:";
    private Connection conn;
    SearchBackPressureMetricsSnapShot snapshot;

    ArrayList<String> required_searchbp_dims =
            new ArrayList<String>() {
                {
                    // Shard/Task Stats Cancellation Count
                    this.add(
                            AllMetrics.SearchBackPressureStatsValue
                                    .SEARCHBP_SHARD_TASK_STATS_CANCELLATION_COUNT
                                    .toString());
                    this.add(
                            AllMetrics.SearchBackPressureStatsValue
                                    .SEARCHBP_SEARCH_TASK_STATS_CANCELLATION_COUNT
                                    .toString());
                    this.add(
                            AllMetrics.SearchBackPressureStatsValue
                                    .SEARCHBP_SHARD_TASK_STATS_COMPLETION_COUNT
                                    .toString());
                    this.add(
                            AllMetrics.SearchBackPressureStatsValue
                                    .SEARCHBP_SEARCH_TASK_STATS_COMPLETION_COUNT
                                    .toString());

                    // Shard Stats Resource Heap / CPU Usage
                    this.add(
                            AllMetrics.SearchBackPressureStatsValue
                                    .SEARCHBP_SHARD_TASK_STATS_RESOURCE_HEAP_USAGE_CANCELLATION_COUNT
                                    .toString());
                    this.add(
                            AllMetrics.SearchBackPressureStatsValue
                                    .SEARCHBP_SHARD_TASK_STATS_RESOURCE_HEAP_USAGE_CURRENT_MAX
                                    .toString());
                    this.add(
                            AllMetrics.SearchBackPressureStatsValue
                                    .SEARCHBP_SHARD_TASK_STATS_RESOURCE_HEAP_USAGE_ROLLING_AVG
                                    .toString());
                    this.add(
                            AllMetrics.SearchBackPressureStatsValue
                                    .SEARCHBP_SHARD_TASK_STATS_RESOURCE_CPU_USAGE_CANCELLATION_COUNT
                                    .toString());
                    this.add(
                            AllMetrics.SearchBackPressureStatsValue
                                    .SEARCHBP_SHARD_TASK_STATS_RESOURCE_CPU_USAGE_CURRENT_MAX
                                    .toString());
                    this.add(
                            AllMetrics.SearchBackPressureStatsValue
                                    .SEARCHBP_SHARD_TASK_STATS_RESOURCE_CPU_USAGE_CURRENT_AVG
                                    .toString());

                    // Task Stats Resource Heap / CPU Usage
                    this.add(
                            AllMetrics.SearchBackPressureStatsValue
                                    .SEARCHBP_SEARCH_TASK_STATS_RESOURCE_HEAP_USAGE_CANCELLATION_COUNT
                                    .toString());
                    this.add(
                            AllMetrics.SearchBackPressureStatsValue
                                    .SEARCHBP_SEARCH_TASK_STATS_RESOURCE_HEAP_USAGE_CURRENT_MAX
                                    .toString());
                    this.add(
                            AllMetrics.SearchBackPressureStatsValue
                                    .SEARCHBP_SEARCH_TASK_STATS_RESOURCE_HEAP_USAGE_ROLLING_AVG
                                    .toString());
                    this.add(
                            AllMetrics.SearchBackPressureStatsValue
                                    .SEARCHBP_SEARCH_TASK_STATS_RESOURCE_CPU_USAGE_CANCELLATION_COUNT
                                    .toString());
                    this.add(
                            AllMetrics.SearchBackPressureStatsValue
                                    .SEARCHBP_SEARCH_TASK_STATS_RESOURCE_CPU_USAGE_CURRENT_MAX
                                    .toString());
                    this.add(
                            AllMetrics.SearchBackPressureStatsValue
                                    .SEARCHBP_SEARCH_TASK_STATS_RESOURCE_CPU_USAGE_CURRENT_AVG
                                    .toString());
                }
            };

    @Before
    public void setup() throws Exception {
        Class.forName("org.sqlite.JDBC");
        System.setProperty("java.io.tmpdir", "/tmp");
        conn = DriverManager.getConnection(DB_URL);
        snapshot = new SearchBackPressureMetricsSnapShot(conn, System.currentTimeMillis());
    }

    @Test
    public void testReadSearchBackPressureMetricsSnapshot() throws Exception {
        final BatchBindStep handle = snapshot.startBatchPut();
        insertIntoTable(handle);

        final Result<Record> result = snapshot.fetchAll();

        assertEquals(1, result.size());
        // for 14 (length of required_searchbp_dims) fields, each assign a value from 0 to 13
        // test each field and verify the result
        for (int i = 0; i < required_searchbp_dims.size(); i++) {
            Assert.assertEquals(
                    AllMetrics.SearchBackPressureStatsValue
                                    .SEARCHBP_SHARD_TASK_STATS_CANCELLATION_COUNT
                                    .toString()
                            + " should be "
                            + String.valueOf(i),
                    i,
                    ((Number) result.get(0).get(required_searchbp_dims.get(i))).intValue());
        }
    }

    @After
    public void tearDown() throws Exception {
        conn.close();
    }

    private void insertIntoTable(BatchBindStep handle) {
        Object[] bindVals = new Object[required_searchbp_dims.size()];
        for (int i = 0; i < required_searchbp_dims.size(); i++) {
            bindVals[i] = Integer.valueOf(i);
        }

        handle.bind(bindVals).execute();
    }
}
