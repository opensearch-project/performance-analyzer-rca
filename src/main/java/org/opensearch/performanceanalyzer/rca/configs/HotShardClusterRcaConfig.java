/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.configs;


import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

public class HotShardClusterRcaConfig {
    public static final String CONFIG_NAME = "hot-shard-cluster-rca";

    private Double cpuUtilizationClusterThreshold;
    private Double heapAllocRateClusterThreshold;

    public static final double DEFAULT_CPU_UTILIZATION_CLUSTER_THRESHOLD = 0.3;
    public static final double DEFAULT_HEAP_ALLOC_RATE_CLUSTER_THRESHOLD = 0.3;

    public HotShardClusterRcaConfig(final RcaConf rcaConf) {
        cpuUtilizationClusterThreshold =
                rcaConf.readRcaConfig(
                        CONFIG_NAME,
                        HotShardClusterRcaConfig.RCA_CONF_KEY_CONSTANTS
                                .CPU_UTILIZATION_CLUSTER_THRESHOLD,
                        DEFAULT_CPU_UTILIZATION_CLUSTER_THRESHOLD,
                        (s) -> (s > 0),
                        Double.class);
        heapAllocRateClusterThreshold =
                rcaConf.readRcaConfig(
                        CONFIG_NAME,
                        HotShardClusterRcaConfig.RCA_CONF_KEY_CONSTANTS
                                .HEAP_ALLOC_RATE_CLUSTER_THRESHOLD,
                        DEFAULT_HEAP_ALLOC_RATE_CLUSTER_THRESHOLD,
                        (s) -> (s > 0),
                        Double.class);
    }

    public double getCpuUtilizationClusterThreshold() {
        return cpuUtilizationClusterThreshold;
    }

    public double getHeapAllocRateClusterThreshold() {
        return heapAllocRateClusterThreshold;
    }

    public static class RCA_CONF_KEY_CONSTANTS {
        private static final String CPU_UTILIZATION_CLUSTER_THRESHOLD =
                "cpu-utilization-cluster-percentage";
        private static final String HEAP_ALLOC_RATE_CLUSTER_THRESHOLD =
                "heap-alloc-rate-cluster-percentage";
    }
}
