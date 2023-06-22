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
        LOG.info("SearchBackPressureMetricsProcessor initialized");
        this.searchBackPressureMetricsSnapShot = searchBackPressureMetricsSnapShot;
    }

    // path for the metrics in shared folder
    // public static final String searchbp_path = "search_back_pressure";

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
        LOG.info("handleSearchBackPressureEvent start to parse evnt: " + eventValue);
        // 0thline is current time string (e.g. {current_time:1686952296889})
        // 1st line is the payload the metrics
        if (lines.length < 2) {
            throw new RuntimeException("Missing SearchBackPressure Metrics payload and timestamp.");
            //     return;
        }

        // Parse metrics payload
        parseJsonLine(lines[1]);
    }

    private void parseJsonLine(final String jsonString) {
        //
        Map<String, Object> map = JsonConverter.createMapFrom(jsonString);
        LOG.info("SearchBackPressureMetricsProcessor parseJsonLine: {}", jsonString);

        if (map.isEmpty()) {
            throw new RuntimeException("Missing SearchBackPressure Metrics payload.");
            //     return;
        }
        // A list of dims to be collected
        ArrayList<String> required_searchbp_dims =
                new ArrayList<String>() {
                    {
                        // Shard/Task Stats Cancellation Count
                        this.add(
                                AllMetrics.SearchBackPressureStatsValue
                                        .SEARCHBP_SHARD_STATS_CANCELLATIONCOUNT
                                        .toString());
                        this.add(
                                AllMetrics.SearchBackPressureStatsValue
                                        .SEARCHBP_TASK_STATS_CANCELLATIONCOUNT
                                        .toString());

                        // Shard Stats Resource Heap / CPU Usage
                        this.add(
                                AllMetrics.SearchBackPressureStatsValue
                                        .SEARCHBP_SHARD_STATS_RESOURCE_HEAP_USAGE_CANCELLATIONCOUNT
                                        .toString());
                        this.add(
                                AllMetrics.SearchBackPressureStatsValue
                                        .SEARCHBP_SHARD_STATS_RESOURCE_HEAP_USAGE_CURRENTMAX
                                        .toString());
                        this.add(
                                AllMetrics.SearchBackPressureStatsValue
                                        .SEARCHBP_SHARD_STATS_RESOURCE_HEAP_USAGE_ROLLINGAVG
                                        .toString());
                        this.add(
                                AllMetrics.SearchBackPressureStatsValue
                                        .SEARCHBP_SHARD_STATS_RESOURCE_CPU_USAGE_CANCELLATIONCOUNT
                                        .toString());
                        this.add(
                                AllMetrics.SearchBackPressureStatsValue
                                        .SEARCHBP_SHARD_STATS_RESOURCE_CPU_USAGE_CURRENTMAX
                                        .toString());
                        this.add(
                                AllMetrics.SearchBackPressureStatsValue
                                        .SEARCHBP_SHARD_STATS_RESOURCE_CPU_USAGE_CURRENTAVG
                                        .toString());

                        // Task Stats Resource Heap / CPU Usage
                        this.add(
                                AllMetrics.SearchBackPressureStatsValue
                                        .SEARCHBP_TASK_STATS_RESOURCE_HEAP_USAGE_CANCELLATIONCOUNT
                                        .toString());
                        this.add(
                                AllMetrics.SearchBackPressureStatsValue
                                        .SEARCHBP_TASK_STATS_RESOURCE_HEAP_USAGE_CURRENTMAX
                                        .toString());
                        this.add(
                                AllMetrics.SearchBackPressureStatsValue
                                        .SEARCHBP_TASK_STATS_RESOURCE_HEAP_USAGE_ROLLINGAVG
                                        .toString());
                        this.add(
                                AllMetrics.SearchBackPressureStatsValue
                                        .SEARCHBP_TASK_STATS_RESOURCE_CPU_USAGE_CANCELLATIONCOUNT
                                        .toString());
                        this.add(
                                AllMetrics.SearchBackPressureStatsValue
                                        .SEARCHBP_TASK_STATS_RESOURCE_CPU_USAGE_CURRENTMAX
                                        .toString());
                        this.add(
                                AllMetrics.SearchBackPressureStatsValue
                                        .SEARCHBP_TASK_STATS_RESOURCE_CPU_USAGE_CURRENTAVG
                                        .toString());
                    }
                };

        Object[] bindVals = new Object[required_searchbp_dims.size()];
        int idx = 0;
        for (String dimension : required_searchbp_dims) {
            bindVals[idx++] = map.get(dimension);
            LOG.info(
                    "SearchBackPressureMetricsProcessor field {} parsed is: {}",
                    dimension,
                    map.get(dimension));
        }

        handle.bind(bindVals);
    }

    @Override
    public void processEvent(Event event) {
        // One Handler method for incoming event
        LOG.info("SearchBP processEvent start!");
        handleSearchBackPressureEvent(event.value);

        // commit Batch queries is overflow the limit
        commitBatchIfRequired();
    }
}
