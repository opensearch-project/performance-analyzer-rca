/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class ClusterManager_PendingQueueSize extends Metric {
    public ClusterManager_PendingQueueSize(long evaluationIntervalSeconds) {
        super(
                AllMetrics.ClusterManagerPendingValue.CLUSTER_MANAGER_PENDING_QUEUE_SIZE.name(),
                evaluationIntervalSeconds);
    }
}
