/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.collectors;


import java.util.HashMap;
import java.util.Map;
import org.opensearch.performanceanalyzer.OSMetricsGeneratorFactory;
import org.opensearch.performanceanalyzer.commons.collectors.PerformanceAnalyzerMetricsCollector;
import org.opensearch.performanceanalyzer.commons.metrics.MetricsConfiguration;
import org.opensearch.performanceanalyzer.commons.metrics.MetricsProcessor;
import org.opensearch.performanceanalyzer.commons.metrics.PerformanceAnalyzerMetrics;
import org.opensearch.performanceanalyzer.commons.metrics.WriterMetrics;
import org.opensearch.performanceanalyzer.commons.stats.CommonStats;
import org.opensearch.performanceanalyzer.metrics_generator.DiskMetricsGenerator;
import org.opensearch.performanceanalyzer.metrics_generator.OSMetricsGenerator;

public class DisksCollector extends PerformanceAnalyzerMetricsCollector
        implements MetricsProcessor {

    private static final int sTimeInterval =
            MetricsConfiguration.CONFIG_MAP.get(DisksCollector.class).samplingInterval;

    public DisksCollector() {
        super(sTimeInterval, "DisksCollector");
    }

    @Override
    public String getMetricsPath(long startTime, String... keysPath) {
        // throw exception if keys.length is not equal to 0
        if (keysPath.length != 0) {
            throw new RuntimeException("keys length should be 0");
        }

        return PerformanceAnalyzerMetrics.generatePath(
                startTime, PerformanceAnalyzerMetrics.sDisksPath);
    }

    @Override
    public void collectMetrics(long startTime) {
        OSMetricsGenerator generator = OSMetricsGeneratorFactory.getInstance();
        if (generator == null) {
            return;
        }
        long mCurrT = System.currentTimeMillis();
        DiskMetricsGenerator diskMetricsGenerator = generator.getDiskMetricsGenerator();
        diskMetricsGenerator.addSample();

        saveMetricValues(getMetrics(diskMetricsGenerator), startTime);
        CommonStats.WRITER_METRICS_AGGREGATOR.updateStat(
                WriterMetrics.DISKS_COLLECTOR_EXECUTION_TIME,
                "",
                System.currentTimeMillis() - mCurrT);
    }

    private Map<String, DiskMetrics> getMetricsMap(DiskMetricsGenerator diskMetricsGenerator) {

        Map<String, DiskMetrics> map = new HashMap<>();

        for (String disk : diskMetricsGenerator.getAllDisks()) {
            DiskMetrics diskMetrics = new DiskMetrics();
            diskMetrics.name = disk;
            diskMetrics.await = diskMetricsGenerator.getAwait(disk);
            diskMetrics.serviceRate = diskMetricsGenerator.getServiceRate(disk);
            diskMetrics.utilization = diskMetricsGenerator.getDiskUtilization(disk);

            map.put(disk, diskMetrics);
        }

        return map;
    }

    private String getMetrics(DiskMetricsGenerator diskMetricsGenerator) {

        Map<String, DiskMetrics> map = getMetricsMap(diskMetricsGenerator);
        value.setLength(0);
        value.append(PerformanceAnalyzerMetrics.getJsonCurrentMilliSeconds())
                .append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);
        for (Map.Entry<String, DiskMetrics> entry : map.entrySet()) {
            value.append(entry.getValue().serialize())
                    .append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);
        }
        return value.toString();
    }
}
