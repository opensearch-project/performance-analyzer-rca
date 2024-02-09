/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;

import java.sql.Connection;
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

public class GarbageCollectorInfoProcessor implements EventProcessor {

    private static final Logger LOG = LogManager.getLogger(GarbageCollectorInfoProcessor.class);

    private GarbageCollectorInfoSnapshot gcSnap;
    private BatchBindStep handle;
    private long startTime;
    private long endTime;

    private GarbageCollectorInfoProcessor(GarbageCollectorInfoSnapshot gcSnap) {
        this.gcSnap = gcSnap;
    }

    static GarbageCollectorInfoProcessor buildGarbageCollectorInfoProcessor(
            long currWindowStartTime,
            Connection conn,
            NavigableMap<Long, GarbageCollectorInfoSnapshot> gcInfoMap) {
        if (gcInfoMap.get(currWindowStartTime) == null) {
            GarbageCollectorInfoSnapshot gcSnap =
                    new GarbageCollectorInfoSnapshot(conn, currWindowStartTime);
            gcInfoMap.put(currWindowStartTime, gcSnap);

            return new GarbageCollectorInfoProcessor(gcSnap);
        }

        return new GarbageCollectorInfoProcessor(gcInfoMap.get(currWindowStartTime));
    }

    @Override
    public void initializeProcessing(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.handle = gcSnap.startBatchPut();
    }

    @Override
    public void finalizeProcessing() {
        if (handle.size() > 0) {
            handle.execute();
        }
    }

    @Override
    public void processEvent(Event event) {
        handleGarbageCollectorInfoEvent(event);
        commitBatchIfRequired();
    }

    private void handleGarbageCollectorInfoEvent(Event event) {
        String[] lines = event.value.split(System.getProperty("line.separator"));
        // first line is the timestamp
        for (int i = 1; i < lines.length; ++i) {
            parseJsonLine(lines[i]);
        }
    }

    private void parseJsonLine(final String jsonString) {
        Map<String, Object> map = JsonConverter.createMapFrom(jsonString);
        if (map.isEmpty()) {
            LOG.warn("Empty line in the event log for gc_info section.");
            return;
        }

        AllMetrics.GCInfoDimension[] dims = AllMetrics.GCInfoDimension.values();

        Object[] bindVals = new Object[dims.length];
        int idx = 0;
        for (AllMetrics.GCInfoDimension dimension : dims) {
            bindVals[idx++] = map.get(dimension.toString());
        }

        handle.bind(bindVals);
    }

    @Override
    public boolean shouldProcessEvent(Event event) {
        return event.key.contains(PerformanceAnalyzerMetrics.sGcInfoPath);
    }

    @Override
    public void commitBatchIfRequired() {
        if (handle.size() >= BATCH_LIMIT) {
            handle.execute();
            handle = gcSnap.startBatchPut();
        }
    }
}
