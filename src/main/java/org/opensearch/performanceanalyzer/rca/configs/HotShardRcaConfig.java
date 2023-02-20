/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.configs;


import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

public class HotShardRcaConfig {
    public static final String CONFIG_NAME = "hot-shard-rca";

    private final Double cpuUtilizationThreshold;
    private final Double heapAllocRateThreshold;

    public static final double DEFAULT_CPU_UTILIZATION_THRESHOLD = 0.01;
    public static final double DEFAULT_HEAP_ALLOC_RATE_THRESHOLD_IN_BYTE_PER_SEC = 2500.0;

    public HotShardRcaConfig(final RcaConf rcaConf) {
        cpuUtilizationThreshold =
                rcaConf.readRcaConfig(
                        CONFIG_NAME,
                        HotShardRcaConfig.RCA_CONF_KEY_CONSTANTS.CPU_UTILIZATION_THRESHOLD,
                        DEFAULT_CPU_UTILIZATION_THRESHOLD,
                        (s) -> (s > 0),
                        Double.class);
        heapAllocRateThreshold =
                rcaConf.readRcaConfig(
                        CONFIG_NAME,
                        HotShardRcaConfig.RCA_CONF_KEY_CONSTANTS
                                .HEAP_ALLOC_RATE_THRESHOLD_IN_BYTES,
                        DEFAULT_HEAP_ALLOC_RATE_THRESHOLD_IN_BYTE_PER_SEC,
                        (s) -> (s > 0),
                        Double.class);
    }

    public double getCpuUtilizationThreshold() {
        return cpuUtilizationThreshold;
    }

    public double getHeapAllocRateThreshold() {
        return heapAllocRateThreshold;
    }

    public static class RCA_CONF_KEY_CONSTANTS {
        public static final String CPU_UTILIZATION_THRESHOLD = "cpu-utilization";
        public static final String HEAP_ALLOC_RATE_THRESHOLD_IN_BYTES =
                "heap-alloc-rate-in-bytes";
    }
}
