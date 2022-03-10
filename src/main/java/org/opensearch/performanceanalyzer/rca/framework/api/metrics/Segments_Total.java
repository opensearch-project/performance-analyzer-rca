/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Segments_Total extends Metric {
    public Segments_Total(long evaluationIntervalSeconds) {
        super(AllMetrics.ShardStatsValue.SEGMENTS_TOTAL.name(), evaluationIntervalSeconds);
    }
}
