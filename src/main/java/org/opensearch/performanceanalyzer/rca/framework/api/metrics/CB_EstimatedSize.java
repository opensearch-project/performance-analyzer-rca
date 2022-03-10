/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class CB_EstimatedSize extends Metric {
    public CB_EstimatedSize(long evaluationIntervalSeconds) {
        super(AllMetrics.CircuitBreakerValue.CB_ESTIMATED_SIZE.name(), evaluationIntervalSeconds);
    }
}
