/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metrics_generator.linux;


import java.util.HashMap;
import java.util.Map;
import org.opensearch.performanceanalyzer.metrics_generator.DiskIOMetricsGenerator;
import org.opensearch.performanceanalyzer.os.ThreadDiskIO;

public class LinuxDiskIOMetricsGenerator implements DiskIOMetricsGenerator {

    private Map<String, ThreadDiskIO.IOMetrics> diskIOMetricsMap;

    public LinuxDiskIOMetricsGenerator() {
        diskIOMetricsMap = new HashMap<>();
    }

    @Override
    public double getAvgReadThroughputBps(final String threadId) {

        return diskIOMetricsMap.get(threadId).avgReadThroughputBps;
    }

    @Override
    public double getAvgReadSyscallRate(final String threadId) {

        return diskIOMetricsMap.get(threadId).avgReadSyscallRate;
    }

    @Override
    public double getAvgWriteThroughputBps(final String threadId) {

        return diskIOMetricsMap.get(threadId).avgWriteThroughputBps;
    }

    @Override
    public double getAvgWriteSyscallRate(final String threadId) {

        return diskIOMetricsMap.get(threadId).avgWriteSyscallRate;
    }

    @Override
    public double getAvgTotalThroughputBps(final String threadId) {

        return diskIOMetricsMap.get(threadId).avgTotalThroughputBps;
    }

    @Override
    public double getAvgTotalSyscallRate(final String threadId) {

        return diskIOMetricsMap.get(threadId).avgTotalSyscallRate;
    }

    @Override
    public boolean hasDiskIOMetrics(final String threadId) {

        return diskIOMetricsMap.containsKey(threadId);
    }

    @Override
    public void addSample() {
        ThreadDiskIO.addSample();
    }

    public void setDiskIOMetrics(final String threadId, final ThreadDiskIO.IOMetrics ioMetrics) {
        diskIOMetricsMap.put(threadId, ioMetrics);
    }
}
