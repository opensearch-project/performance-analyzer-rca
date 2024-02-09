/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;

import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Latency extends Metric {
    public Latency(long evaluationIntervalSeconds) {
        super(AllMetrics.ShardBulkMetric.LATENCY.name(), evaluationIntervalSeconds);
    }
}
