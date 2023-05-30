/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Indexing_Buffer extends Metric {
    public Indexing_Buffer(long evaluationIntervalSeconds) {
        super(AllMetrics.ShardStatsValue.INDEXING_BUFFER.name(), evaluationIntervalSeconds);
    }
}
