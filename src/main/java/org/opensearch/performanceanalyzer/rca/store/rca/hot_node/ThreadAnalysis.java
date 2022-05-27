/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.hot_node;


import java.util.function.Predicate;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ReaderMetrics;

public class ThreadAnalysis {
    private final ThreadMetricsSlidingWindow blockedTimeWindow;

    private final ThreadMetricsSlidingWindow waitedTimeWindow;
    private final Predicate<String> typeFilter;
    private final ReaderMetrics blockedThreadCountMetric,
            waitedThreadCountMetric,
            maxBlockedTimeMetric,
            maxWaitedTimeMetric;

    public ThreadAnalysis(
            Predicate<String> typeFilter,
            ReaderMetrics blockedThreadCountMetric,
            ReaderMetrics waitedThreadCount,
            ReaderMetrics maxBlockedTime,
            ReaderMetrics maxWaitedTimeMetric) {
        this.typeFilter = typeFilter;
        this.blockedThreadCountMetric = blockedThreadCountMetric;
        this.waitedThreadCountMetric = waitedThreadCount;
        this.maxBlockedTimeMetric = maxBlockedTime;
        this.maxWaitedTimeMetric = maxWaitedTimeMetric;
        blockedTimeWindow = new ThreadMetricsSlidingWindow();
        waitedTimeWindow = new ThreadMetricsSlidingWindow();
    }

    public ThreadMetricsSlidingWindow getBlockedTimeWindow() {
        return blockedTimeWindow;
    }

    public ThreadMetricsSlidingWindow getWaitedTimeWindow() {
        return waitedTimeWindow;
    }

    public Predicate<String> getTypeFilter() {
        return typeFilter;
    }

    public ReaderMetrics getBlockedThreadCountMetric() {
        return blockedThreadCountMetric;
    }

    public ReaderMetrics getWaitedThreadCountMetric() {
        return waitedThreadCountMetric;
    }

    public ReaderMetrics getMaxBlockedTimeMetric() {
        return maxBlockedTimeMetric;
    }

    public ReaderMetrics getMaxWaitedTimeMetric() {
        return maxWaitedTimeMetric;
    }
}
