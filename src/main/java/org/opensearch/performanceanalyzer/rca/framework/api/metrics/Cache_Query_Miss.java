/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;

import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Cache_Query_Miss extends Metric {
    public Cache_Query_Miss(long evaluationIntervalSeconds) {
        super(AllMetrics.ShardStatsValue.CACHE_QUERY_MISS.toString(), evaluationIntervalSeconds);
    }
}
