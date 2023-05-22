/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.collectors;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import java.lang.management.MemoryUsage;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.commons.collectors.PerformanceAnalyzerMetricsCollector;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.GCType;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.HeapDimension;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.HeapValue;
import org.opensearch.performanceanalyzer.commons.metrics.MetricsConfiguration;
import org.opensearch.performanceanalyzer.commons.metrics.MetricsProcessor;
import org.opensearch.performanceanalyzer.commons.metrics.PerformanceAnalyzerMetrics;
import org.opensearch.performanceanalyzer.commons.stats.CommonStats;
import org.opensearch.performanceanalyzer.jvm.GCMetrics;
import org.opensearch.performanceanalyzer.jvm.HeapMetrics;
import org.opensearch.performanceanalyzer.rca.framework.metrics.WriterMetrics;

public class HeapMetricsCollector extends PerformanceAnalyzerMetricsCollector
        implements MetricsProcessor {
    private static final Logger LOG = LogManager.getLogger(HeapMetricsCollector.class);
    public static final int SAMPLING_TIME_INTERVAL =
            MetricsConfiguration.CONFIG_MAP.get(HeapMetricsCollector.class).samplingInterval;
    private static final int KEYS_PATH_LENGTH = 0;

    public HeapMetricsCollector() {
        super(SAMPLING_TIME_INTERVAL, "HeapMetrics");
    }

    @Override
    public void collectMetrics(long startTime) {
        long mCurrT = System.currentTimeMillis();
        GCMetrics.runGCMetrics();

        value.setLength(0);
        value.append(PerformanceAnalyzerMetrics.getJsonCurrentMilliSeconds())
                .append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);
        value.append(
                        new HeapStatus(
                                        GCType.TOT_YOUNG_GC.toString(),
                                        GCMetrics.getTotYoungGCCollectionCount(),
                                        GCMetrics.getTotYoungGCCollectionTime())
                                .serialize())
                .append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);

        value.append(
                        new HeapStatus(
                                        GCType.TOT_FULL_GC.toString(),
                                        GCMetrics.getTotFullGCCollectionCount(),
                                        GCMetrics.getTotFullGCCollectionTime())
                                .serialize())
                .append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);

        for (Map.Entry<String, Supplier<MemoryUsage>> entry :
                HeapMetrics.getMemoryUsageSuppliers().entrySet()) {
            MemoryUsage memoryUsage = entry.getValue().get();

            value.append(
                            new HeapStatus(
                                            entry.getKey(),
                                            memoryUsage.getCommitted(),
                                            memoryUsage.getInit(),
                                            memoryUsage.getMax(),
                                            memoryUsage.getUsed())
                                    .serialize())
                    .append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);
        }

        saveMetricValues(value.toString(), startTime);
        CommonStats.WRITER_METRICS_AGGREGATOR.updateStat(
                WriterMetrics.HEAP_METRICS_COLLECTOR_EXECUTION_TIME,
                "",
                System.currentTimeMillis() - mCurrT);
    }

    @Override
    public String getMetricsPath(long startTime, String... keysPath) {
        // throw exception if keys.length is not equal to 0
        if (keysPath.length != KEYS_PATH_LENGTH) {
            throw new RuntimeException("keys length should be " + KEYS_PATH_LENGTH);
        }

        return PerformanceAnalyzerMetrics.generatePath(
                startTime, PerformanceAnalyzerMetrics.sHeapPath);
    }

    public static class HeapStatus extends MetricStatus {
        // GC type like survivor
        private String type;

        // -2 means this metric is undefined for a memory pool.  For example,
        // The memory pool Eden has no collectionCount metric.
        @VisibleForTesting static final long UNDEFINED = -2;

        // the total number of collections that have occurred
        private long collectionCount = UNDEFINED;

        // the approximate accumulated collection elapsed time in milliseconds
        private long collectionTime = UNDEFINED;

        // the amount of memory in bytes that is committed for the Java virtual machine to use
        private long committed = UNDEFINED;

        // the amount of memory in bytes that the Java virtual machine initially requests from the
        // operating system for memory management
        private long init = UNDEFINED;

        // the maximum amount of memory in bytes that can be used for memory management
        private long max = UNDEFINED;

        // the amount of used memory in bytes
        private long used = UNDEFINED;

        // Allows for automatic JSON deserialization
        public HeapStatus() {}

        public HeapStatus(String type, long collectionCount, long collectionTime) {
            this.type = type;
            this.collectionCount = collectionCount;
            this.collectionTime = collectionTime;
        }

        public HeapStatus(String type, long committed, long init, long max, long used) {

            this.type = type;
            this.committed = committed;
            this.init = init;
            this.max = max;
            this.used = used;
        }

        @JsonProperty(HeapDimension.Constants.TYPE_VALUE)
        public String getType() {
            return type;
        }

        @JsonProperty(HeapValue.Constants.COLLECTION_COUNT_VALUE)
        public long getCollectionCount() {
            return collectionCount;
        }

        @JsonProperty(HeapValue.Constants.COLLECTION_TIME_VALUE)
        public long getCollectionTime() {
            return collectionTime;
        }

        @JsonProperty(HeapValue.Constants.COMMITTED_VALUE)
        public long getCommitted() {
            return committed;
        }

        @JsonProperty(HeapValue.Constants.INIT_VALUE)
        public long getInit() {
            return init;
        }

        @JsonProperty(HeapValue.Constants.MAX_VALUE)
        public long getMax() {
            return max;
        }

        @JsonProperty(HeapValue.Constants.USED_VALUE)
        public long getUsed() {
            return used;
        }
    }
}
