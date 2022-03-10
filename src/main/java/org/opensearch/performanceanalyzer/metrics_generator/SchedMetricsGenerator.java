/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metrics_generator;

public interface SchedMetricsGenerator {

    // This method will be called before all following get methods
    // to make sure that all information exists for a thread id
    boolean hasSchedMetrics(String threadId);

    double getAvgRuntime(String threadId);

    double getAvgWaittime(String threadId);

    double getContextSwitchRate(String threadId);

    void addSample();
}
