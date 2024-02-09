/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;

import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

/**
 * This metric is calculated from the Node Stat Metrics for a particular node and returns the per
 * Shard ID and Index Name dimensional shard sizes. This metric is aggregated over all shards in
 * different RCAs(Temperature Profile RCA).
 */
public class ShardSize extends Metric {
    public static final String NAME = AllMetrics.ShardStatsValue.SHARD_SIZE_IN_BYTES.toString();

    public ShardSize(long evaluationIntervalSeconds) {
        super(AllMetrics.ShardStatsValue.SHARD_SIZE_IN_BYTES.name(), evaluationIntervalSeconds);
    }
}
