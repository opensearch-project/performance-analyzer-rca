/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Map;
import java.util.NavigableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.BatchBindStep;
import org.opensearch.performanceanalyzer.commons.event_process.Event;
import org.opensearch.performanceanalyzer.commons.event_process.EventProcessor;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.commons.metrics.PerformanceAnalyzerMetrics;
import org.opensearch.performanceanalyzer.commons.util.JsonConverter;

public class SearchBackPressureMetricsProcessor implements EventProcessor {

    private static final Logger LOG =
            LogManager.getLogger(SearchBackPressureMetricsProcessor.class);

    // instance of SearchBackPressureMetricsSnapShot to interact with the backend db
    private SearchBackPressureMetricsSnapShot searchBackPressureMetricsSnapShot;

    // entry point for batch queries
    private BatchBindStep handle;

    // normally starTime and endTime are gapped by 5 seconds (default sampling interval)
    private long startTime;
    private long endTime;

    private SearchBackPressureMetricsProcessor(
            SearchBackPressureMetricsSnapShot searchBackPressureMetricsSnapShot) {
        this.searchBackPressureMetricsSnapShot = searchBackPressureMetricsSnapShot;
    }

    /*
     * if current SnapShotMap has the snapshot for currentWindowStartTime, use the snapshot to build the processor
     * else create a new Instance of SearchBackPressureMetricsSnapShot to initialize the processor
     */
    static SearchBackPressureMetricsProcessor buildSearchBackPressureMetricsProcessor(
            long currentWindowStartTime,
            Connection connection,
            NavigableMap<Long, SearchBackPressureMetricsSnapShot>
                    searchBackPressureSnapshotNavigableMap) {
        // if current metrics is in searchBackPressureSnapshotNavigableMap map
        if (searchBackPressureSnapshotNavigableMap.get(currentWindowStartTime) == null) {
            SearchBackPressureMetricsSnapShot searchBackPressureMetricsSnapShot =
                    new SearchBackPressureMetricsSnapShot(connection, currentWindowStartTime);
            searchBackPressureSnapshotNavigableMap.put(
                    currentWindowStartTime, searchBackPressureMetricsSnapShot);
            return new SearchBackPressureMetricsProcessor(searchBackPressureMetricsSnapShot);
        }
        return new SearchBackPressureMetricsProcessor(
                searchBackPressureSnapshotNavigableMap.get(currentWindowStartTime));
    }

    @Override
    public void initializeProcessing(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.handle = searchBackPressureMetricsSnapShot.startBatchPut();
    }

    @Override
    public void finalizeProcessing() {
        if (handle.size() > 0) {
            handle.execute();
        }
    }

    @Override
    public boolean shouldProcessEvent(Event event) {
        return event.key.contains(PerformanceAnalyzerMetrics.sSearchBackPressureMetricsPath);
    }

    @Override
    public void commitBatchIfRequired() {
        if (handle.size() >= BATCH_LIMIT) {
            handle.execute();
            handle = searchBackPressureMetricsSnapShot.startBatchPut();
        }
    }

    // Handler method for incoming events
    private void handleSearchBackPressureEvent(String eventValue) {
        String[] lines = eventValue.split(System.lineSeparator());
        if (lines.length < 2) {
            throw new RuntimeException("Missing SearchBackPressure Metrics payload and timestamp.");
        }

        // Parse metrics payload
        parseJsonLine(lines[1]);
    }

    private void parseJsonLine(final String jsonString) {
        Map<String, Object> map = JsonConverter.createMapFrom(jsonString);

        if (map.isEmpty()) {
            throw new RuntimeException("Missing SearchBackPressure Metrics payload.");
        }

        // A list of dims to be collected
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

        Object[] bindVals = new Object[required_searchbp_dims.size()];
        int idx = 0;
        for (String dimension : required_searchbp_dims) {
            bindVals[idx++] = map.get(dimension);
        }

        handle.bind(bindVals);
    }

    @Override
    public void processEvent(Event event) {
        // Handler method for incoming event
        handleSearchBackPressureEvent(event.value);

        // commit Batch queries is overflow the limit
        commitBatchIfRequired();
    }
}
