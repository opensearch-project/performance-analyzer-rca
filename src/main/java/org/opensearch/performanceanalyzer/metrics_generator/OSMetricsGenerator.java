/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metrics_generator;


import java.util.Set;

public interface OSMetricsGenerator {

    String getPid();

    CPUPagingActivityGenerator getPagingActivityGenerator();

    SchedMetricsGenerator getSchedMetricsGenerator();

    Set<String> getAllThreadIds();

    DiskIOMetricsGenerator getDiskIOMetricsGenerator();

    TCPMetricsGenerator getTCPMetricsGenerator();

    IPMetricsGenerator getIPMetricsGenerator();

    DiskMetricsGenerator getDiskMetricsGenerator();

    MountedPartitionMetricsGenerator getMountedPartitionMetricsGenerator();
}
