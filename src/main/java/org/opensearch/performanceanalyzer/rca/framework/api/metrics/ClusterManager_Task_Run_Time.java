/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;

import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class ClusterManager_Task_Run_Time extends Metric {
    public ClusterManager_Task_Run_Time(long evaluationIntervalSeconds) {
        super(
                AllMetrics.ClusterManagerMetricDimensions.CLUSTER_MANAGER_TASK_RUN_TIME.name(),
                evaluationIntervalSeconds);
    }
}
