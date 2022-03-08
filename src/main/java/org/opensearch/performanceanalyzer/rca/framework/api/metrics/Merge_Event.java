/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Merge_Event extends Metric {
    public Merge_Event(long evaluationIntervalSeconds) {
        super(AllMetrics.ShardStatsValue.MERGE_EVENT.name(), evaluationIntervalSeconds);
    }
}
