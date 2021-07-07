/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 * Copyright 2019-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
import org.opensearch.performanceanalyzer.rca.framework.metrics.ExceptionsAndErrors;

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
        IPMetricsGenerator IPMetricsGenerator = generator.getIPMetricsGenerator();
        IPMetricsGenerator.addSample();
        saveMetricValues(
                getMetrics(IPMetricsGenerator) + PerformanceAnalyzerMetrics.sMetricNewLineDelimitor,
                startTime);
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
            PerformanceAnalyzerApp.ERRORS_AND_EXCEPTIONS_AGGREGATOR.updateStat(
                    ExceptionsAndErrors.NETWORK_COLLECTOR_ERROR, "", 1);
            LOG.debug(
                    "Exception in NetworkInterfaceCollector: {} with ExceptionCode: {}",
                    () -> e.toString(),
                    () -> ExceptionsAndErrors.NETWORK_COLLECTOR_ERROR.toString());
        }

        return value.toString();
    }
}
