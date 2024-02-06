/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;

import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class CB_ConfiguredSize extends Metric {
    public CB_ConfiguredSize(long evaluationIntervalSeconds) {
        super(AllMetrics.CircuitBreakerValue.CB_CONFIGURED_SIZE.name(), evaluationIntervalSeconds);
    }
}
