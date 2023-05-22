/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class CB_TrippedEvents extends Metric {
    public CB_TrippedEvents(long evaluationIntervalSeconds) {
        super(AllMetrics.CircuitBreakerValue.CB_TRIPPED_EVENTS.name(), evaluationIntervalSeconds);
    }
}
