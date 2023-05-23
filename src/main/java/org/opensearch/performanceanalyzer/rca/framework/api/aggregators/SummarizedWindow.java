/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.aggregators;


import java.util.concurrent.TimeUnit;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;

/* Specific class for HotShard analysis. */
public class SummarizedWindow {
    private static final long tickExtension = 5000L;
    protected double sumCpuUtilization = 0.0;
    protected double sumHeapAllocRate = 0.0;
    protected long timeStampDistant = 0;
    protected long timeStampRecent = 0;

    protected void reset() {
        this.timeStampDistant = this.timeStampRecent = 0L;
        this.sumHeapAllocRate = this.sumCpuUtilization = 0.0;
    }

    public void next(AllMetrics.OSMetrics metricType, double addend, long nextTimeStamp) {
        if (AllMetrics.OSMetrics.CPU_UTILIZATION.equals(metricType)) {
            this.sumCpuUtilization += addend;
        } else {
            this.sumHeapAllocRate += addend;
        }

        if (this.timeStampDistant == 0L) {
            this.timeStampDistant = nextTimeStamp;
        } else {
            this.timeStampRecent = nextTimeStamp;
        }
    }

    public double readAvgMetricValue(TimeUnit timeUnit, AllMetrics.OSMetrics metricType) {
        if (AllMetrics.OSMetrics.CPU_UTILIZATION.equals(metricType)) {
            return readAvgCpuUtilization(timeUnit);
        } else {
            return readAvgHeapAllocRate(timeUnit);
        }
    }

    public double readAvgCpuUtilization(TimeUnit timeUnit) {
        if (empty()) {
            return Double.NaN;
        }
        long timestampDiff = timeStampRecent - timeStampDistant + tickExtension;
        return sumCpuUtilization / ((double) timestampDiff / (double) timeUnit.toMillis(1));
    }

    public double readAvgHeapAllocRate(TimeUnit timeUnit) {
        if (empty()) {
            return Double.NaN;
        }
        long timestampDiff = timeStampRecent - timeStampDistant + tickExtension;
        return sumHeapAllocRate / ((double) timestampDiff / (double) timeUnit.toMillis(1));
    }

    private boolean empty() {
        return this.timeStampDistant == 0L;
    }
}
