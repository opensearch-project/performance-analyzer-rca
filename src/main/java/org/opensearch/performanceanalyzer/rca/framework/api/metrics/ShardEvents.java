/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;

import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class ShardEvents extends Metric {
    public ShardEvents(long evaluationIntervalSeconds) {
        super(AllMetrics.ShardOperationMetric.SHARD_OP_COUNT.name(), evaluationIntervalSeconds);
    }
}
