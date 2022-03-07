/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metrics_generator;

public interface IPMetricsGenerator {
    double getInPacketRate4();

    double getOutPacketRate4();

    double getInDropRate4();

    double getOutDropRate4();

    double getInPacketRate6();

    double getOutPacketRate6();

    double getInDropRate6();

    double getOutDropRate6();

    double getInBps();

    double getOutBps();

    void addSample();
}
