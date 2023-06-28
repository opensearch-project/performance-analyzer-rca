/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca;


import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.opensearch.performanceanalyzer.commons.collectors.StatsCollector;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.commons.stats.metrics.StatExceptionCode;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.SlidingWindow;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.SlidingWindowData;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.persist.SQLParsingUtil;

public abstract class OldGenRca<T extends ResourceFlowUnit<?>> extends Rca<T> {

    private static final Logger LOG = LogManager.getLogger(OldGenRca.class);
    private static final double CONVERT_BYTES_TO_MEGABYTES = Math.pow(1024, 2);
    private static final String CMS_COLLECTOR = "ConcurrentMarkSweep";
    protected final Metric heap_Used;
    protected final Metric heap_Max;
    protected final Metric gc_event;
    protected final Metric gc_type;

    public OldGenRca(
            long evaluationIntervalSeconds,
            Metric heapUsed,
            Metric heapMax,
            Metric gcEvent,
            Metric gcType) {
        super(evaluationIntervalSeconds);
        this.heap_Max = heapMax;
        this.heap_Used = heapUsed;
        this.gc_event = gcEvent;
        this.gc_type = gcType;
    }

    protected double getMaxHeapSizeOrDefault(final double defaultValue) {
        if (heap_Max == null) {
            StatsCollector.instance()
                    .logException(StatExceptionCode.MISCONFIGURED_OLD_GEN_RCA_HEAP_MAX_MISSING);
            throw new IllegalStateException(
                    "RCA: "
                            + this.name()
                            + "was not configured in the graph to "
                            + "take heap_Max as a metric. Please check the analysis graph!");
        }

        double maxHeapSize = defaultValue;
        final List<MetricFlowUnit> heapMaxMetrics = heap_Max.getFlowUnits();
        for (MetricFlowUnit heapMaxMetric : heapMaxMetrics) {
            if (heapMaxMetric.isEmpty()) {
                continue;
            }
            double ret =
                    SQLParsingUtil.readDataFromSqlResult(
                            heapMaxMetric.getData(),
                            AllMetrics.HeapDimension.MEM_TYPE.getField(),
                            AllMetrics.GCType.HEAP.toString(),
                            MetricsDB.MAX);
            if (Double.isNaN(ret)) {
                LOG.error(
                        "Failed to parse metric in FlowUnit from {}",
                        heap_Max.getClass().getName());
            } else {
                maxHeapSize = ret / CONVERT_BYTES_TO_MEGABYTES;
            }
        }

        return maxHeapSize;
    }

    protected double getMaxOldGenSizeOrDefault(final double defaultValue) {
        if (heap_Max == null) {
            StatsCollector.instance()
                    .logException(StatExceptionCode.MISCONFIGURED_OLD_GEN_RCA_HEAP_MAX_MISSING);
            throw new IllegalStateException(
                    "RCA: "
                            + this.name()
                            + "was not configured in the graph to "
                            + "take heap_Max as a metric. Please check the analysis graph!");
        }

        double maxOldGenHeapSize = defaultValue;
        final List<MetricFlowUnit> heapMaxMetrics = heap_Max.getFlowUnits();
        for (MetricFlowUnit heapMaxMetric : heapMaxMetrics) {
            if (heapMaxMetric.isEmpty()) {
                continue;
            }
            double ret =
                    SQLParsingUtil.readDataFromSqlResult(
                            heapMaxMetric.getData(),
                            AllMetrics.HeapDimension.MEM_TYPE.getField(),
                            AllMetrics.GCType.OLD_GEN.toString(),
                            MetricsDB.MAX);
            if (Double.isNaN(ret)) {
                LOG.error(
                        "Failed to parse metric in FlowUnit from {}",
                        heap_Max.getClass().getName());
            } else {
                maxOldGenHeapSize = ret / CONVERT_BYTES_TO_MEGABYTES;
            }
        }

        return maxOldGenHeapSize;
    }

    protected int getFullGcEventsOrDefault(final double defaultValue) {
        if (gc_event == null) {
            StatsCollector.instance()
                    .logException(StatExceptionCode.MISCONFIGURED_OLD_GEN_RCA_GC_EVENTS_MISSING);
            throw new IllegalStateException(
                    "RCA: "
                            + this.name()
                            + "was not configured in the graph to "
                            + "take gc_event as a metric. Please check the analysis graph!");
        }

        double fullGcEvents = defaultValue;
        final List<MetricFlowUnit> gcEventMetrics = gc_event.getFlowUnits();

        for (MetricFlowUnit gcEventMetric : gcEventMetrics) {
            if (gcEventMetric.isEmpty()) {
                continue;
            }
            double ret =
                    SQLParsingUtil.readDataFromSqlResult(
                            gcEventMetric.getData(),
                            AllMetrics.HeapDimension.MEM_TYPE.getField(),
                            AllMetrics.GCType.TOT_FULL_GC.toString(),
                            MetricsDB.MAX);
            if (Double.isNaN(ret)) {
                LOG.error(
                        "Failed to parse metric in FlowUnit from {}",
                        gc_event.getClass().getName());
            } else {
                fullGcEvents = ret;
            }
        }

        return (int) fullGcEvents;
    }

    protected double getOldGenUsedOrDefault(final double defaultValue) {
        if (heap_Used == null) {
            StatsCollector.instance()
                    .logException(StatExceptionCode.MISCONFIGURED_OLD_GEN_RCA_HEAP_USED_MISSING);
            throw new IllegalStateException(
                    "RCA: "
                            + this.name()
                            + "was not configured in the graph to "
                            + "take heap_Used as a metric. Please check the analysis graph!");
        }

        final List<MetricFlowUnit> heapUsedMetrics = heap_Used.getFlowUnits();
        double oldGenHeapUsed = defaultValue;
        for (MetricFlowUnit heapUsedMetric : heapUsedMetrics) {
            if (heapUsedMetric.isEmpty()) {
                continue;
            }
            double ret =
                    SQLParsingUtil.readDataFromSqlResult(
                            heapUsedMetric.getData(),
                            AllMetrics.HeapDimension.MEM_TYPE.getField(),
                            AllMetrics.GCType.OLD_GEN.toString(),
                            MetricsDB.MAX);
            if (Double.isNaN(ret)) {
                LOG.error(
                        "Failed to parse metric in FlowUnit from {}",
                        heap_Used.getClass().getName());
            } else {
                oldGenHeapUsed = ret / CONVERT_BYTES_TO_MEGABYTES;
            }
        }

        return oldGenHeapUsed;
    }

    protected boolean isOldGenCollectorCMS() {
        if (gc_type == null) {
            throw new IllegalStateException(
                    "RCA: "
                            + this.name()
                            + "was not configured in the graph to "
                            + "take GC_Type as a metric. Please check the analysis graph!");
        }

        final List<MetricFlowUnit> gcTypeFlowUnits = gc_type.getFlowUnits();
        Field<String> memTypeField = AllMetrics.GCInfoDimension.MEMORY_POOL.getField();
        Field<String> collectorField = AllMetrics.GCInfoDimension.COLLECTOR_NAME.getField();
        for (MetricFlowUnit gcTypeFlowUnit : gcTypeFlowUnits) {
            if (gcTypeFlowUnit.isEmpty()) {
                continue;
            }

            Result<Record> records = gcTypeFlowUnit.getData();
            for (final Record record : records) {
                final String memType = record.get(memTypeField);
                if (AllMetrics.GCType.OLD_GEN.toString().equals(memType)) {
                    return CMS_COLLECTOR.equals(record.get(collectorField));
                }
            }
        }

        // We want to return true here because we don't want to hold up evaluation of RCAs due to
        // transient metric delays. We don't want to tune the JVM only when we know for sure that
        // the
        // collector is not CMS, in all other cases, give JVM RCAs the benefit of the doubt.
        return true;
    }

    /** Sliding window to check the minimal olg gen usage within a given time frame */
    public static class MinOldGenSlidingWindow extends SlidingWindow<SlidingWindowData> {

        public MinOldGenSlidingWindow(int SLIDING_WINDOW_SIZE_IN_TIMESTAMP, TimeUnit timeUnit) {
            super(SLIDING_WINDOW_SIZE_IN_TIMESTAMP, timeUnit);
        }

        @Override
        public void next(SlidingWindowData e) {
            while (!windowDeque.isEmpty() && windowDeque.peekFirst().getValue() >= e.getValue()) {
                windowDeque.pollFirst();
            }
            windowDeque.addFirst(e);
            while (!windowDeque.isEmpty()
                    && TimeUnit.MILLISECONDS.toSeconds(
                                    e.getTimeStamp() - windowDeque.peekLast().getTimeStamp())
                            > SLIDING_WINDOW_SIZE) {
                windowDeque.pollLast();
            }
        }

        public double readMin() {
            if (!windowDeque.isEmpty()) {
                return windowDeque.peekLast().getValue();
            }
            return Double.NaN;
        }
    }

    /**
     * Sliding window to check the max/min olg gen usage within a given time frame Previous
     * MinGoldGenSlidingWindow should be deprecated since it modify the sliding window size in
     * next()
     */
    public static class MinMaxOldGenSlidingWindow extends SlidingWindow<SlidingWindowData> {

        public MinMaxOldGenSlidingWindow(int SLIDING_WINDOW_SIZE_IN_TIMESTAMP, TimeUnit timeUnit) {
            super(SLIDING_WINDOW_SIZE_IN_TIMESTAMP, timeUnit);
        }

        public double readMax() {
            if (!windowDeque.isEmpty()) {
                return windowDeque.stream()
                        .mapToDouble(SlidingWindowData::getValue)
                        .max()
                        .orElse(Double.NaN);
            }
            return Double.NaN;
        }

        public double readMin() {
            if (!windowDeque.isEmpty()) {
                return windowDeque.stream()
                        .mapToDouble(SlidingWindowData::getValue)
                        .min()
                        .orElse(Double.NaN);
            }
            return Double.NaN;
        }
    }
}
