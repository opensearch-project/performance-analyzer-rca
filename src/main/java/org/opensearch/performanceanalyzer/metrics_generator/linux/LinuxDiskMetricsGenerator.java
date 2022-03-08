/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metrics_generator.linux;


import java.util.Map;
import java.util.Set;
import org.opensearch.performanceanalyzer.collectors.DiskMetrics;
import org.opensearch.performanceanalyzer.hwnet.Disks;
import org.opensearch.performanceanalyzer.metrics_generator.DiskMetricsGenerator;

public class LinuxDiskMetricsGenerator implements DiskMetricsGenerator {

    private Map<String, DiskMetrics> diskMetricsMap;

    @Override
    public Set<String> getAllDisks() {
        return diskMetricsMap.keySet();
    }

    @Override
    public double getDiskUtilization(final String disk) {

        return diskMetricsMap.get(disk).utilization;
    }

    @Override
    public double getAwait(final String disk) {

        return diskMetricsMap.get(disk).await;
    }

    @Override
    public double getServiceRate(final String disk) {

        return diskMetricsMap.get(disk).serviceRate;
    }

    @Override
    public void addSample() {
        Disks.addSample();
    }

    public void setDiskMetricsMap(final Map<String, DiskMetrics> map) {

        diskMetricsMap = map;
    }
}
