/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.collectors;


import java.util.Set;
import org.opensearch.performanceanalyzer.OSMetricsGeneratorFactory;
import org.opensearch.performanceanalyzer.commons.collectors.PerformanceAnalyzerMetricsCollector;
import org.opensearch.performanceanalyzer.commons.metrics.MetricsConfiguration;
import org.opensearch.performanceanalyzer.commons.metrics.MetricsProcessor;
import org.opensearch.performanceanalyzer.commons.metrics.PerformanceAnalyzerMetrics;
import org.opensearch.performanceanalyzer.metrics_generator.MountedPartitionMetricsGenerator;
import org.opensearch.performanceanalyzer.metrics_generator.OSMetricsGenerator;

public class MountedPartitionMetricsCollector extends PerformanceAnalyzerMetricsCollector
        implements MetricsProcessor {

    private static final int SAMPLING_TIME_INTERVAL =
            MetricsConfiguration.CONFIG_MAP.get(MountedPartitionMetricsCollector.class)
                    .samplingInterval;
    private static final int EXPECTED_KEYS_PATH_LENGTH = 0;

    public MountedPartitionMetricsCollector() {
        super(SAMPLING_TIME_INTERVAL, "MountedPartition");
    }

    @Override
    public void collectMetrics(long startTime) {
        OSMetricsGenerator generator = OSMetricsGeneratorFactory.getInstance();
        if (generator == null) {
            return;
        }
        MountedPartitionMetricsGenerator mountedPartitionMetricsGenerator =
                generator.getMountedPartitionMetricsGenerator();

        mountedPartitionMetricsGenerator.addSample();

        saveMetricValues(getMetrics(mountedPartitionMetricsGenerator), startTime);
    }

    @Override
    public String getMetricsPath(long startTime, String... keysPath) {
        if (keysPath != null && keysPath.length != EXPECTED_KEYS_PATH_LENGTH) {
            throw new RuntimeException("keys length should be " + EXPECTED_KEYS_PATH_LENGTH);
        }

        return PerformanceAnalyzerMetrics.generatePath(
                startTime, PerformanceAnalyzerMetrics.sMountedPartitionMetricsPath);
    }

    private String getMetrics(
            final MountedPartitionMetricsGenerator mountedPartitionMetricsGenerator) {
        // zero the string builder
        value.setLength(0);

        // first line is the timestamp
        value.append(PerformanceAnalyzerMetrics.getJsonCurrentMilliSeconds())
                .append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);

        Set<String> mountPoints = mountedPartitionMetricsGenerator.getAllMountPoints();
        for (String mountPoint : mountPoints) {
            value.append(
                            new MountedPartitionMetrics(
                                            mountedPartitionMetricsGenerator.getDevicePartition(
                                                    mountPoint),
                                            mountPoint,
                                            mountedPartitionMetricsGenerator.getTotalSpace(
                                                    mountPoint),
                                            mountedPartitionMetricsGenerator.getFreeSpace(
                                                    mountPoint),
                                            mountedPartitionMetricsGenerator.getUsableFreeSpace(
                                                    mountPoint))
                                    .serialize())
                    .append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);
        }
        return value.toString();
    }
}
