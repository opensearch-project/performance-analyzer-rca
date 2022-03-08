/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metrics_generator;


import java.util.Set;

public interface TCPMetricsGenerator {

    Set<String> getAllDestionationIps();

    int getNumberOfFlows(String ip);

    double getTransmitQueueSize(String ip);

    double getReceiveQueueSize(String ip);

    double getCurrentLost(String ip);

    double getSendCongestionWindow(String ip);

    double getSlowStartThreshold(String ip);

    void addSample();
}
