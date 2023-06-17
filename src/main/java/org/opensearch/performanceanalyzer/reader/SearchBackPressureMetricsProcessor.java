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

    // path for the metrics in shared folder
    public static final String searchbp_path = "search_back_pressure";

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
        return event.key.contains(searchbp_path);
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
        // 0th line is the headline like ^search_back_pressure
        // 1st line is current time string (e.g. current_time:1686952296889)
        // 2nd line is the payload the metrics
        if (lines.length < 2) {
            LOG.warn("SearchBackPressure metrics length should be at least 2");
            return;
        }

        for (int i = 2; i < lines.length; ++i) {
            parseJsonLine(lines[i]);
        }
    }

    private void parseJsonLine(final String jsonString) {
        Map<String, Object> map = JsonConverter.createMapFrom(jsonString);
        String searchbp_mode = "searchbp_mode";
        if (map.isEmpty()) {
            LOG.warn("Empty line in the event log for search back pressure section.");
            return;
        }

        // AllMetrics.GCInfoDimension[] dims = AllMetrics.GCInfoDimension.values();
        // A list of dims to be collected
        ArrayList<String> required_searchbp_dims =
                new ArrayList<String>() {
                    {
                        this.add(searchbp_mode);
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
        // One Handler method for incoming event
        handleSearchBackPressureEvent(event.value);

        // commit Batch queries is overflow the limit
        commitBatchIfRequired();
    }
}
