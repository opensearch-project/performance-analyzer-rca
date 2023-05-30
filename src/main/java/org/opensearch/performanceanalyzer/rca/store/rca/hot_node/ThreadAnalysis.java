/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.hot_node;


import java.util.function.Predicate;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaRuntimeMetrics;

public class ThreadAnalysis {
    private final ThreadMetricsSlidingWindow blockedTimeWindow;

    private final ThreadMetricsSlidingWindow waitedTimeWindow;
    private final Predicate<String> typeFilter;
    private final RcaRuntimeMetrics blockedThreadCountMetric,
            waitedThreadCountMetric,
            maxBlockedTimeMetric,
            maxWaitedTimeMetric;

    public ThreadAnalysis(
            Predicate<String> typeFilter,
            RcaRuntimeMetrics blockedThreadCountMetric,
            RcaRuntimeMetrics waitedThreadCount,
            RcaRuntimeMetrics maxBlockedTime,
            RcaRuntimeMetrics maxWaitedTimeMetric) {
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

    public RcaRuntimeMetrics getBlockedThreadCountMetric() {
        return blockedThreadCountMetric;
    }

    public RcaRuntimeMetrics getWaitedThreadCountMetric() {
        return waitedThreadCountMetric;
    }

    public RcaRuntimeMetrics getMaxBlockedTimeMetric() {
        return maxBlockedTimeMetric;
    }

    public RcaRuntimeMetrics getMaxWaitedTimeMetric() {
        return maxWaitedTimeMetric;
    }
}
