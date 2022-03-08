/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Terms_Memory extends Metric {
    public Terms_Memory(long evaluationIntervalSeconds) {
        super(AllMetrics.ShardStatsValue.TERMS_MEMORY.name(), evaluationIntervalSeconds);
    }
}
