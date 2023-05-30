/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metricsdb.Metric;

public class PublishClusterState_Failure extends Metric {
    public PublishClusterState_Failure(long evaluationIntervalSeconds) {
        super(
                AllMetrics.ClusterManagerClusterUpdateStatsValue.PUBLISH_CLUSTER_STATE_FAILURE
                        .name(),
                evaluationIntervalSeconds);
    }
}
