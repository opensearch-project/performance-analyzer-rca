/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metricsdb.Metric;

public class PublishClusterState_Latency extends Metric {
    public PublishClusterState_Latency(long evaluationIntervalSeconds) {
        super(
                AllMetrics.ClusterManagerClusterUpdateStatsValue.PUBLISH_CLUSTER_STATE_LATENCY
                        .name(),
                evaluationIntervalSeconds);
    }
}
