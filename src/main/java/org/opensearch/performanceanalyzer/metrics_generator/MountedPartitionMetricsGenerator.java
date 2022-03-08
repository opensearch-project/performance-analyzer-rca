/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metrics_generator;


import java.util.Set;

public interface MountedPartitionMetricsGenerator {
    void addSample();

    Set<String> getAllMountPoints();

    String getDevicePartition(String mountPoint);

    long getTotalSpace(String mountPoint);

    long getFreeSpace(String mountPoint);

    long getUsableFreeSpace(String mountPoint);
}
