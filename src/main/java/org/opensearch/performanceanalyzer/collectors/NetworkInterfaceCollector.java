/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.collectors;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.OSMetricsGeneratorFactory;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.metrics.MetricsConfiguration;
import org.opensearch.performanceanalyzer.metrics.MetricsProcessor;
import org.opensearch.performanceanalyzer.metrics.PerformanceAnalyzerMetrics;
import org.opensearch.performanceanalyzer.metrics_generator.IPMetricsGenerator;
import org.opensearch.performanceanalyzer.metrics_generator.OSMetricsGenerator;
import org.opensearch.performanceanalyzer.rca.framework.metrics.WriterMetrics;

public class NetworkInterfaceCollector extends PerformanceAnalyzerMetricsCollector
        implements MetricsProcessor {
    private static final int sTimeInterval =
            MetricsConfiguration.CONFIG_MAP.get(NetworkInterfaceCollector.class).samplingInterval;
    private static final Logger LOG = LogManager.getLogger(NetworkInterfaceCollector.class);

    public NetworkInterfaceCollector() {
        super(sTimeInterval, "NetworkInterfaceCollector");
    }

    @Override
    public void collectMetrics(long startTime) {
        OSMetricsGenerator generator = OSMetricsGeneratorFactory.getInstance();
        if (generator == null) {
            return;
        }

        long mCurrT = System.currentTimeMillis();
        IPMetricsGenerator IPMetricsGenerator = generator.getIPMetricsGenerator();
        IPMetricsGenerator.addSample();
        saveMetricValues(
                getMetrics(IPMetricsGenerator) + PerformanceAnalyzerMetrics.sMetricNewLineDelimitor,
                startTime);
        PerformanceAnalyzerApp.WRITER_METRICS_AGGREGATOR.updateStat(
                WriterMetrics.NETWORK_INTERFACE_COLLECTOR_EXECUTION_TIME,
                "",
                System.currentTimeMillis() - mCurrT);
    }

    @Override
    public String getMetricsPath(long startTime, String... keysPath) {
        // throw exception if keys.length is not equal to 0
        if (keysPath.length != 0) {
            throw new RuntimeException("keys length should be 0");
        }

        return PerformanceAnalyzerMetrics.generatePath(
                startTime, PerformanceAnalyzerMetrics.sIPPath);
    }

    private String getMetrics(IPMetricsGenerator IPMetricsGenerator) {

        value.setLength(0);
        value.append(PerformanceAnalyzerMetrics.getJsonCurrentMilliSeconds())
                .append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);

        try {
            NetInterfaceSummary inNetwork =
                    new NetInterfaceSummary(
                            NetInterfaceSummary.Direction.in,
                            IPMetricsGenerator.getInPacketRate4(),
                            IPMetricsGenerator.getInDropRate4(),
                            IPMetricsGenerator.getInPacketRate6(),
                            IPMetricsGenerator.getInDropRate6(),
                            IPMetricsGenerator.getInBps());

            NetInterfaceSummary outNetwork =
                    new NetInterfaceSummary(
                            NetInterfaceSummary.Direction.out,
                            IPMetricsGenerator.getOutPacketRate4(),
                            IPMetricsGenerator.getOutDropRate4(),
                            IPMetricsGenerator.getOutPacketRate6(),
                            IPMetricsGenerator.getOutDropRate6(),
                            IPMetricsGenerator.getOutBps());

            value.append(inNetwork.serialize())
                    .append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);
            value.append(outNetwork.serialize())
                    .append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);
        } catch (Exception e) {
            LOG.debug(
                    "Exception in NetworkInterfaceCollector: {} with ExceptionCode: {}",
                    () -> e.toString(),
                    () -> StatExceptionCode.NETWORK_COLLECTION_ERROR.toString());
            StatsCollector.instance().logException(StatExceptionCode.NETWORK_COLLECTION_ERROR);
        }

        return value.toString();
    }
}
