/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Merge_Time extends Metric {
    public Merge_Time(long evaluationIntervalSeconds) {
        super(AllMetrics.ShardStatsValue.MERGE_TIME.name(), evaluationIntervalSeconds);
    }
}
