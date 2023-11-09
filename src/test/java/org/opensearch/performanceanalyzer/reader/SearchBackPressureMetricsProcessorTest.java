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
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.commons.metrics.PerformanceAnalyzerMetrics;

public class SearchBackPressureMetricsProcessorTest {
    private static final String DB_URL = "jdbc:sqlite:";
    // private static final String TEST_MEM_POOL = "testMemPool";
    // private static final String COLLECTOR_NAME = "testCollectorName";
    private static final String SEARCH_BACK_PRESSURE_STATS_KEY = "search_back_pressure_stats";
    private SearchBackPressureMetricsProcessor searchBackPressureMetricsProcessor;
    private long currTimeStamp;

    private NavigableMap<Long, SearchBackPressureMetricsSnapShot> searchBackPressureStatsMap;
    Connection conn;

    // mock SearchBackPressureStatsCollector to test Event processing
    private static final String SERIALIZED_EVENT =
            "{\"searchbp_shard_stats_cancellationCount\":2,"
                    + "\"searchbp_shard_stats_limitReachedCount\":2,"
                    + "\"searchbp_shard_stats_completionCount\": 10,"
                    + "\"searchbp_shard_stats_resource_heap_usage_cancellationCount\":3,"
                    + "\"searchbp_shard_stats_resource_heap_usage_currentMax\":3,"
                    + "\"searchbp_shard_stats_resource_heap_usage_rollingAvg\":3,"
                    + "\"searchbp_shard_stats_resource_cpu_usage_cancellationCount\":5,"
                    + "\"searchbp_shard_stats_resource_cpu_usage_currentMax\":5,"
                    + "\"searchbp_shard_stats_resource_cpu_usage_currentAvg\":5,"
                    + "\"searchbp_shard_stats_resource_elaspedtime_usage_cancellationCount\":2,"
                    + "\"searchbp_shard_stats_resource_elaspedtime_usage_currentMax\":2,"
                    + "\"searchbp_shard_stats_resource_elaspedtime_usage_currentAvg\":2,"
                    + "\"searchbp_task_stats_cancellationCount\":0,"
                    + "\"searchbp_task_stats_limitReachedCount\":0,"
                    + "\"searchbp_task_stats_completionCount\": 5,"
                    + "\"searchbp_task_stats_resource_heap_usage_cancellationCount\":0,"
                    + "\"searchbp_task_stats_resource_heap_usage_currentMax\":0,"
                    + "\"searchbp_task_stats_resource_heap_usage_rollingAvg\":0,"
                    + "\"searchbp_task_stats_resource_cpu_usage_cancellationCount\":0,"
                    + "\"searchbp_task_stats_resource_cpu_usage_currentMax\":0,"
                    + "\"searchbp_task_stats_resource_cpu_usage_currentAvg\":0,"
                    + "\"searchbp_task_stats_resource_elaspedtime_usage_cancellationCount\":0,"
                    + "\"searchbp_task_stats_resource_elaspedtime_usage_currentMax\":0,"
                    + "\"searchbp_task_stats_resource_elaspedtime_usage_currentAvg\":0,"
                    + "\"searchbp_mode\":\"MONITOR_ONLY\","
                    + "\"searchbp_nodeid\":\"FgNAAAQQQDSROABCDEFHTX\"}";

    @Before
    public void setup() throws Exception {
        Class.forName("org.sqlite.JDBC");
        System.setProperty("java.io.tmpdir", "/tmp");
        conn = DriverManager.getConnection(DB_URL);
        this.currTimeStamp = System.currentTimeMillis();
        this.searchBackPressureStatsMap = new TreeMap<>();
        this.searchBackPressureMetricsProcessor =
                searchBackPressureMetricsProcessor.buildSearchBackPressureMetricsProcessor(
                        currTimeStamp, conn, searchBackPressureStatsMap);
    }

    // Test valid case of the handleSearchBackPressureEvent()
    @Test
    public void testSearchBackPressureProcessEvent() throws Exception {
        // Create a SearchBackPressureEvent
        Event testEvent = buildTestSearchBackPressureStatsEvent();

        // Test the SearchBackPressureMetricsSnapShot
        searchBackPressureMetricsProcessor.initializeProcessing(
                this.currTimeStamp, System.currentTimeMillis());
        assertTrue(searchBackPressureMetricsProcessor.shouldProcessEvent(testEvent));

        searchBackPressureMetricsProcessor.processEvent(testEvent);
        searchBackPressureMetricsProcessor.finalizeProcessing();

        SearchBackPressureMetricsSnapShot currSnapshot =
                searchBackPressureStatsMap.get(this.currTimeStamp);
        Result<Record> result = currSnapshot.fetchAll();
        assertEquals(1, result.size());

        // SEARCHBP_SHARD_STATS_RESOURCE_HEAP_USAGE_ROLLINGAVG value is 3L according to the
        // SERIALIZED_EVENT, should EQUAL
        Assert.assertEquals(
                3L,
                result.get(0)
                        .get(
                                AllMetrics.SearchBackPressureStatsValue
                                        .SEARCHBP_SHARD_STATS_RESOURCE_HEAP_USAGE_ROLLINGAVG
                                        .toString()));
        // SEARCHBP_TASK_STATS_RESOURCE_CPU_USAGE_CANCELLATIONCOUNT value is 0L according to the
        // SERIALIZED_EVENT, should EQUAL
        Assert.assertEquals(
                0,
                result.get(0)
                        .get(
                                AllMetrics.SearchBackPressureStatsValue
                                        .SEARCHBP_TASK_STATS_RESOURCE_CPU_USAGE_CANCELLATIONCOUNT
                                        .toString()));

        // SEARCHBP_TASK_STATS_RESOURCE_CPU_USAGE_CANCELLATIONCOUNT value is 0L according to the
        // SERIALIZED_EVENT, should NOT EQUAL
        Assert.assertNotEquals(
                2,
                result.get(0)
                        .get(
                                AllMetrics.SearchBackPressureStatsValue
                                        .SEARCHBP_TASK_STATS_RESOURCE_CPU_USAGE_CANCELLATIONCOUNT
                                        .toString()));
    }

    @Test
    public void testEmptySearchBackPressureProcessEvent() throws Exception {
        // Create a SearchBackPressureEvent
        Event testEvent = buildEmptyTestSearchBackPressureStatsEvent();

        // Test the SearchBackPressureMetricsSnapShot
        searchBackPressureMetricsProcessor.initializeProcessing(
                this.currTimeStamp, System.currentTimeMillis());
        assertTrue(searchBackPressureMetricsProcessor.shouldProcessEvent(testEvent));

        try {
            searchBackPressureMetricsProcessor.processEvent(testEvent);
            Assert.assertFalse(
                    "Negative scenario test: Should catch a RuntimeException and skip this test",
                    true);
        } catch (RuntimeException ex) {
            // should catch the exception and the previous assertion should not be executed
        }
    }

    private Event buildTestSearchBackPressureStatsEvent() {
        StringBuilder str = new StringBuilder();
        str.append(PerformanceAnalyzerMetrics.getJsonCurrentMilliSeconds())
                .append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);

        str.append(SERIALIZED_EVENT).append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);
        return new Event(
                SEARCH_BACK_PRESSURE_STATS_KEY, str.toString(), System.currentTimeMillis());
    }

    private Event buildEmptyTestSearchBackPressureStatsEvent() {
        StringBuilder str = new StringBuilder();
        str.append(PerformanceAnalyzerMetrics.getJsonCurrentMilliSeconds())
                .append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);

        return new Event(
                SEARCH_BACK_PRESSURE_STATS_KEY, str.toString(), System.currentTimeMillis());
    }
}
