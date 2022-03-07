/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metrics_generator;


import java.util.Set;

public interface DiskMetricsGenerator {
    Set<String> getAllDisks();

    double getDiskUtilization(String disk);

    double getAwait(String disk);

    double getServiceRate(String disk);

    void addSample();
}
