/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Map;
import java.util.NavigableMap;
import org.jooq.BatchBindStep;
import org.opensearch.performanceanalyzer.commons.event_process.Event;
import org.opensearch.performanceanalyzer.commons.event_process.EventProcessor;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.commons.metrics.PerformanceAnalyzerMetrics;
import org.opensearch.performanceanalyzer.commons.util.JsonConverter;

public class AdmissionControlProcessor implements EventProcessor {

    private AdmissionControlSnapshot admissionControlSnapshot;
    private BatchBindStep handle;
    private long startTime;
    private long endTime;

    private AdmissionControlProcessor(AdmissionControlSnapshot admissionControlSnapshot) {
        this.admissionControlSnapshot = admissionControlSnapshot;
    }

    static AdmissionControlProcessor build(
            long currentWindowStartTime,
            Connection connection,
            NavigableMap<Long, AdmissionControlSnapshot> admissionControlSnapshotNavigableMap) {

        if (admissionControlSnapshotNavigableMap.get(currentWindowStartTime) == null) {
            AdmissionControlSnapshot admissionControlSnapshot =
                    new AdmissionControlSnapshot(connection, currentWindowStartTime);
            admissionControlSnapshotNavigableMap.put(
                    currentWindowStartTime, admissionControlSnapshot);
            return new AdmissionControlProcessor(admissionControlSnapshot);
        }

        return new AdmissionControlProcessor(
                admissionControlSnapshotNavigableMap.get(currentWindowStartTime));
    }

    @Override
    public void initializeProcessing(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.handle = admissionControlSnapshot.startBatchPut();
    }

    @Override
    public void finalizeProcessing() {
        if (handle.size() > 0) {
            handle.execute();
        }
    }

    @Override
    public void processEvent(Event event) {
        processEvent(event.value);
        if (handle.size() == 500) {
            handle.execute();
            handle = admissionControlSnapshot.startBatchPut();
        }
    }

    private void processEvent(String eventValue) {
        String[] lines = eventValue.split(System.lineSeparator());
        Arrays.stream(lines)
                .forEach(
                        line -> {
                            Map<String, Object> map = JsonConverter.createMapFrom(line);
                            String controller =
                                    (String)
                                            map.get(
                                                    AllMetrics.AdmissionControlDimension.Constants
                                                            .CONTROLLER_NAME);
                            long rejectionCount = getRejectionCount(map);
                            handle.bind(controller, rejectionCount);
                        });
    }

    private long getRejectionCount(Map<String, Object> map) {
        Object rejectionCountObject =
                map.get(AllMetrics.AdmissionControlValue.Constants.REJECTION_COUNT);
        return rejectionCountObject instanceof String
                ? Long.parseLong((String) rejectionCountObject)
                : ((Number) rejectionCountObject).longValue();
    }

    @Override
    public boolean shouldProcessEvent(Event event) {
        return event.key.contains(PerformanceAnalyzerMetrics.sAdmissionControlMetricsPath);
    }

    @Override
    public void commitBatchIfRequired() {
        if (handle.size() >= BATCH_LIMIT) {
            handle.execute();
            handle = admissionControlSnapshot.startBatchPut();
        }
    }
}
