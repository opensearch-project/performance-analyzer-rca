/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metrics;


import org.opensearch.performanceanalyzer.core.Util;

public interface MetricsProcessor {

    default String getMetricValues(long startTime, String... keysPath) {
        return PerformanceAnalyzerMetrics.getMetric(getMetricsPath(startTime, keysPath));
    }

    default void saveMetricValues(String value, long startTime, String... keysPath) {
        Util.invokePrivileged(
                () ->
                        PerformanceAnalyzerMetrics.emitMetric(
                                PerformanceAnalyzerMetrics.getTimeInterval(
                                        startTime, MetricsConfiguration.SAMPLING_INTERVAL),
                                getMetricsPath(startTime, keysPath),
                                value));
    }

    default String getMetricValue(String metricName, long startTime, String... keys) {
        return PerformanceAnalyzerMetrics.extractMetricValue(
                getMetricValues(startTime, keys), metricName);
    }

    String getMetricsPath(long startTime, String... keysPath);
}
