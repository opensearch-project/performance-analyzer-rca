/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Master_PendingQueueSize extends Metric {
    public Master_PendingQueueSize(long evaluationIntervalSeconds) {
        super(
                AllMetrics.MasterPendingValue.MASTER_PENDING_QUEUE_SIZE.name(),
                evaluationIntervalSeconds);
    }
}
