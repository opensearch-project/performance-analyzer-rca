/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Flush_Time extends Metric {
    public Flush_Time(long evaluationIntervalSeconds) {
        super(AllMetrics.ShardStatsValue.FLUSH_TIME.name(), evaluationIntervalSeconds);
    }
}
