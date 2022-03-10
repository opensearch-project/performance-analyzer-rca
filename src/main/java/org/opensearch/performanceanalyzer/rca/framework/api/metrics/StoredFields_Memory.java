/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class StoredFields_Memory extends Metric {
    public StoredFields_Memory(long evaluationIntervalSeconds) {
        super(AllMetrics.ShardStatsValue.STORED_FIELDS_MEMORY.name(), evaluationIntervalSeconds);
    }
}
