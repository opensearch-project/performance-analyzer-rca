/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.formatter;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.opensearch.performanceanalyzer.commons.formatter.Formatter;
import org.opensearch.performanceanalyzer.commons.metrics.MeasurementSet;
import org.opensearch.performanceanalyzer.commons.stats.Statistics;

public class StatsCollectorFormatter implements Formatter {
    StringBuilder formatted;
    String sep = "";
    long startTime;
    long endTime;

    public StatsCollectorFormatter() {
        formatted = new StringBuilder();
    }

    private void format(
            MeasurementSet measurementSet, Statistics aggregationType, String name, Number value) {
        formatted.append(sep);
        formatted.append(measurementSet.getName()).append("=").append(value);
        if (!measurementSet.getUnit().isEmpty()) {
            formatted.append(" ").append(measurementSet.getUnit());
        }
        formatted.append(" ").append("aggr|").append(aggregationType);
        if (!name.isEmpty()) {
            formatted.append(" ").append("key|").append(name);
        }
        sep = ",";
    }

    @Override
    public void formatNamedAggregatedValue(
            MeasurementSet measurementSet, Statistics aggregationType, String name, Number value) {
        format(measurementSet, aggregationType, name, value);
    }

    @Override
    public void formatAggregatedValue(
            MeasurementSet measurementSet, Statistics aggregationType, Number value) {
        format(measurementSet, aggregationType, "", value);
    }

    @Override
    public void setStartAndEndTime(long start, long end) {
        this.startTime = start;
        this.endTime = end;
    }

    public List<StatsCollectorReturn> getAllMetrics() {
        List<StatsCollectorReturn> list = new ArrayList<>();
        StatsCollectorReturn statsCollectorReturn =
                new StatsCollectorReturn(this.startTime, this.endTime);
        statsCollectorReturn.statsdata.put("Metrics", formatted.toString());
        list.add(statsCollectorReturn);

        return list;
    }

    public static class StatsCollectorReturn {
        private Map<String, AtomicInteger> counters;
        private Map<String, String> statsdata;
        private Map<String, Double> latencies;
        private long startTimeMillis;
        private long endTimeMillis;

        public StatsCollectorReturn(long startTimeMillis, long endTimeMillis) {
            counters = new HashMap<>();
            statsdata = new HashMap<>();
            latencies = new HashMap<>();
            this.startTimeMillis = startTimeMillis;
            this.endTimeMillis = endTimeMillis;
        }

        public Map<String, AtomicInteger> getCounters() {
            return counters;
        }

        public Map<String, String> getStatsdata() {
            return statsdata;
        }

        public Map<String, Double> getLatencies() {
            return latencies;
        }

        public long getStartTimeMillis() {
            return startTimeMillis;
        }

        public long getEndTimeMillis() {
            return endTimeMillis;
        }

        public boolean isEmpty() {
            return counters.isEmpty() && statsdata.isEmpty() && latencies.isEmpty();
        }
    }
}
