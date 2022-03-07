/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Paging_MinfltRate extends Metric {
    public Paging_MinfltRate(long evaluationIntervalSeconds) {
        super(AllMetrics.OSMetrics.PAGING_MIN_FLT_RATE.name(), evaluationIntervalSeconds);
    }
}
