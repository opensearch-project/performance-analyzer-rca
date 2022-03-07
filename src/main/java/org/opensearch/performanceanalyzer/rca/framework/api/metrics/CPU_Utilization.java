/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class CPU_Utilization extends Metric {
    public static final String NAME = AllMetrics.OSMetrics.CPU_UTILIZATION.toString();

    public CPU_Utilization(long evaluationIntervalSeconds) {
        super(NAME, evaluationIntervalSeconds);
    }
}
