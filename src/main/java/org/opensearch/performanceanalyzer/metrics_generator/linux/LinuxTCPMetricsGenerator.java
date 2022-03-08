/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metrics_generator.linux;


import java.util.Map;
import java.util.Set;
import org.opensearch.performanceanalyzer.hwnet.NetworkE2E;
import org.opensearch.performanceanalyzer.metrics_generator.TCPMetricsGenerator;

public class LinuxTCPMetricsGenerator implements TCPMetricsGenerator {

    private Map<String, double[]> map;

    @Override
    public Set<String> getAllDestionationIps() {
        return map.keySet();
    }

    @Override
    public int getNumberOfFlows(final String ip) {
        return (int) map.get(ip)[0];
    }

    @Override
    public double getTransmitQueueSize(String ip) {
        return map.get(ip)[1];
    }

    @Override
    public double getReceiveQueueSize(String ip) {
        return map.get(ip)[2];
    }

    @Override
    public double getCurrentLost(String ip) {
        return map.get(ip)[3];
    }

    @Override
    public double getSendCongestionWindow(String ip) {
        return map.get(ip)[4];
    }

    @Override
    public double getSlowStartThreshold(String ip) {
        return map.get(ip)[5];
    }

    @Override
    public void addSample() {
        NetworkE2E.addSample();
    }

    public void setTCPMetrics(final Map<String, double[]> metrics) {
        map = metrics;
    }
}
