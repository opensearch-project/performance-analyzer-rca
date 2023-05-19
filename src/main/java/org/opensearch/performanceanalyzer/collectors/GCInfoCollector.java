/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.collectors;


import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.function.Supplier;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.commons.metrics.MetricsConfiguration;
import org.opensearch.performanceanalyzer.commons.metrics.MetricsProcessor;
import org.opensearch.performanceanalyzer.commons.metrics.PerformanceAnalyzerMetrics;
import org.opensearch.performanceanalyzer.jvm.GarbageCollectorInfo;
import org.opensearch.performanceanalyzer.metrics.AllMetrics.GCInfoDimension;
import org.opensearch.performanceanalyzer.rca.framework.metrics.WriterMetrics;

/**
 * A collector that collects info about the current garbage collectors for various regions in the
 * heap.
 */
public class GCInfoCollector extends PerformanceAnalyzerMetricsCollector
        implements MetricsProcessor {

    private static final int SAMPLING_TIME_INTERVAL =
            MetricsConfiguration.CONFIG_MAP.get(GCInfoCollector.class).samplingInterval;
    private static final int EXPECTED_KEYS_PATH_LENGTH = 0;

    public GCInfoCollector() {
        super(SAMPLING_TIME_INTERVAL, "GCInfo");
    }

    @Override
    void collectMetrics(long startTime) {
        long mCurrT = System.currentTimeMillis();
        // Zero the string builder
        value.setLength(0);

        // first line is the timestamp
        value.append(PerformanceAnalyzerMetrics.getJsonCurrentMilliSeconds())
                .append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);

        for (Map.Entry<String, Supplier<String>> entry :
                GarbageCollectorInfo.getGcSuppliers().entrySet()) {
            value.append(new GCInfo(entry.getKey(), entry.getValue().get()).serialize())
                    .append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);
        }

        saveMetricValues(value.toString(), startTime);
        PerformanceAnalyzerApp.WRITER_METRICS_AGGREGATOR.updateStat(
                WriterMetrics.GC_INFO_COLLECTOR_EXECUTION_TIME,
                "",
                System.currentTimeMillis() - mCurrT);
    }

    @Override
    public String getMetricsPath(long startTime, String... keysPath) {
        if (keysPath != null && keysPath.length != EXPECTED_KEYS_PATH_LENGTH) {
            throw new RuntimeException("keys length should be " + EXPECTED_KEYS_PATH_LENGTH);
        }

        return PerformanceAnalyzerMetrics.generatePath(
                startTime, PerformanceAnalyzerMetrics.sGcInfoPath);
    }

    public static class GCInfo extends MetricStatus {
        private String memoryPool;
        private String collectorName;

        public GCInfo() {}

        public GCInfo(final String memoryPool, final String collectorName) {
            this.memoryPool = memoryPool;
            this.collectorName = collectorName;
        }

        @JsonProperty(GCInfoDimension.Constants.MEMORY_POOL_VALUE)
        public String getMemoryPool() {
            return memoryPool;
        }

        @JsonProperty(GCInfoDimension.Constants.COLLECTOR_NAME_VALUE)
        public String getCollectorName() {
            return collectorName;
        }
    }
}
