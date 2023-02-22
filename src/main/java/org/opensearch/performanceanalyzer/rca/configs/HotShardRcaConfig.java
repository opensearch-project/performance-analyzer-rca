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

    private final Integer maxConsumersToSend;

    public static final double DEFAULT_CPU_UTILIZATION_THRESHOLD = 0.01;
    public static final double DEFAULT_HEAP_ALLOC_RATE_THRESHOLD_IN_BYTE_PER_SEC = 250000.0;
    public static final int DEFAULT_MAXIMUM_CONSUMERS_TO_SEND = 50;

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
                        HotShardRcaConfig.RCA_CONF_KEY_CONSTANTS.HEAP_ALLOC_RATE_THRESHOLD_IN_BYTES,
                        DEFAULT_HEAP_ALLOC_RATE_THRESHOLD_IN_BYTE_PER_SEC,
                        (s) -> (s > 0),
                        Double.class);
        maxConsumersToSend =
                rcaConf.readRcaConfig(
                        CONFIG_NAME,
                        HotShardRcaConfig.RCA_CONF_KEY_CONSTANTS.MAX_CONSUMERS_TO_SEND,
                        DEFAULT_MAXIMUM_CONSUMERS_TO_SEND,
                        (s) -> (s > 0),
                        Integer.class);
    }

    public double getCpuUtilizationThreshold() {
        return cpuUtilizationThreshold;
    }

    public double getHeapAllocRateThreshold() {
        return heapAllocRateThreshold;
    }

    public int getMaximumConsumersToSend() {
        return maxConsumersToSend;
    }

    public static class RCA_CONF_KEY_CONSTANTS {
        public static final String CPU_UTILIZATION_THRESHOLD = "cpu-utilization";
        public static final String HEAP_ALLOC_RATE_THRESHOLD_IN_BYTES = "heap-alloc-rate-in-bytes";
        public static final String MAX_CONSUMERS_TO_SEND = "max-consumers-to-send";
    }
}
