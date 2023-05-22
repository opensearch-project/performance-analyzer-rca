/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Indexing_ThrottleTime extends Metric {
    public Indexing_ThrottleTime(long evaluationIntervalSeconds) {
        super(AllMetrics.ShardStatsValue.INDEXING_THROTTLE_TIME.name(), evaluationIntervalSeconds);
    }
}
