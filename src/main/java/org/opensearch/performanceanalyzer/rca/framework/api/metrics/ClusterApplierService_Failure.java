/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class ClusterApplierService_Failure extends Metric {
    public ClusterApplierService_Failure(long evaluationIntervalSeconds) {
        super(
                AllMetrics.ClusterApplierServiceStatsValue.CLUSTER_APPLIER_SERVICE_FAILURE.name(),
                evaluationIntervalSeconds);
    }
}
