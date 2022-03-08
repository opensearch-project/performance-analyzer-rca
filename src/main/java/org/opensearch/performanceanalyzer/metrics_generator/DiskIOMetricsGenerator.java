/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metrics_generator;

public interface DiskIOMetricsGenerator {

    // This method will be called before all following get methods
    // to make sure that all information exists for a thread id
    boolean hasDiskIOMetrics(String threadId);

    // these metrics include page cache activity;
    // only explicit syscalls: NO mmaps (majflts include mmaps)
    double getAvgReadThroughputBps(String threadId);

    double getAvgWriteThroughputBps(String threadId);

    double getAvgTotalThroughputBps(String threadId);

    double getAvgReadSyscallRate(String threadId);

    double getAvgWriteSyscallRate(String threadId);

    double getAvgTotalSyscallRate(String threadId);

    void addSample();
}
