/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.aggregators;


import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.store.rca.hotshard.IndexShardKey;

/* Specific class for HotShard analysis. */
public class SummarizedWindow {
    protected double sumCpuUtilization = 0.0;
    protected double sumHeapAllocRate = 0.0;
    protected long timeStampDistant = 0;
    protected long timeStampRecent = 0;

    protected void reset() {
        this.timeStampDistant = this.timeStampRecent = 0;
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
        return sumCpuUtilization
                / (double) (timeStampRecent - timeStampDistant)
                / (double) timeUnit.toMillis(1);
    }

    public double readAvgHeapAllocRate(TimeUnit timeUnit) {
        return sumHeapAllocRate
                / (double) (timeStampRecent - timeStampDistant)
                / (double) timeUnit.toMillis(1);
    }
}
