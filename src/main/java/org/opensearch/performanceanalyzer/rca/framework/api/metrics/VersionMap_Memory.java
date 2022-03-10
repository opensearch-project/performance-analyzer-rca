/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class VersionMap_Memory extends Metric {
    public VersionMap_Memory(long evaluationIntervalSeconds) {
        super(AllMetrics.ShardStatsValue.VERSION_MAP_MEMORY.name(), evaluationIntervalSeconds);
    }
}
