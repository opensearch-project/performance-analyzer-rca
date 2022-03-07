/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Segments_Memory extends Metric {
    public Segments_Memory(long evaluationIntervalSeconds) {
        super(AllMetrics.ShardStatsValue.SEGMENTS_MEMORY.name(), evaluationIntervalSeconds);
    }
}
