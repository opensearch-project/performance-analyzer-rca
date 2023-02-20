package org.opensearch.performanceanalyzer.rca.framework.api.aggregators;

import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.store.rca.hotshard.IndexShardKey;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;

/* Specific class for HotShard analysis. */
public class SummarizedWindow {
    public static long recycleTimestamp = -1;
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

        if (nextTimeStamp == recycleTimestamp){
            return;
        }

        if (this.timeStampDistant == 0L) {
            this.timeStampDistant = nextTimeStamp;
        } else {
            this.timeStampRecent = nextTimeStamp;
        }
    }

    public double readAvgCpuUtilization(TimeUnit timeUnit) {
        return sumCpuUtilization / (double)(timeStampRecent - timeStampDistant) / (double) timeUnit.toMillis(1);
    }

    public double readAvgHeapAllocRate(TimeUnit timeUnit) {
        return sumHeapAllocRate / (double)(timeStampRecent - timeStampDistant) / (double) timeUnit.toMillis(1);
    }

}
 class NamedSummarizedWindow {
    protected SummarizedWindow summarizedWindow;
    protected IndexShardKey indexShardKey;

    public NamedSummarizedWindow(SummarizedWindow summarizedWindow, IndexShardKey indexShardKey) {
        this.summarizedWindow = summarizedWindow;
        this.indexShardKey = indexShardKey;
    }
}

class SummarizedWindowCPUComparator implements Comparator<NamedSummarizedWindow> {
    @Override
    public int compare(NamedSummarizedWindow o1, NamedSummarizedWindow o2) {
        return Double.compare(
                o2.summarizedWindow.readAvgCpuUtilization(TimeUnit.SECONDS),
                o1.summarizedWindow.readAvgCpuUtilization(TimeUnit.SECONDS)
        );
    }
}

class SummarizedWindowHEAPComparator implements Comparator<NamedSummarizedWindow> {
    @Override
    public int compare(NamedSummarizedWindow o1, NamedSummarizedWindow o2) {
        return Double.compare(
                o2.summarizedWindow.readAvgHeapAllocRate(TimeUnit.SECONDS),
                o1.summarizedWindow.readAvgHeapAllocRate(TimeUnit.SECONDS)
        );
    }
}