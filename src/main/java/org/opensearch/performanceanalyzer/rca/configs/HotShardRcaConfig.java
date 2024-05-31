/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.configs;

import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

public class HotShardRcaConfig {
    public static final String CONFIG_NAME = "hot-shard-rca";

    private final Double cpuUtilizationThreshold;
    private final Integer maxConsumersToSend;

    public static final double DEFAULT_CPU_UTILIZATION_THRESHOLD = 0.015;
    public static final int DEFAULT_TOP_K_CONSUMERS = 50;

    public HotShardRcaConfig(final RcaConf rcaConf) {
        cpuUtilizationThreshold =
                rcaConf.readRcaConfig(
                        CONFIG_NAME,
                        HotShardRcaConfig.RCA_CONF_KEY_CONSTANTS.CPU_UTILIZATION_THRESHOLD,
                        DEFAULT_CPU_UTILIZATION_THRESHOLD,
                        (s) -> (s > 0),
                        Double.class);
        maxConsumersToSend =
                rcaConf.readRcaConfig(
                        CONFIG_NAME,
                        HotShardRcaConfig.RCA_CONF_KEY_CONSTANTS.TOP_K_CONSUMERS,
                        DEFAULT_TOP_K_CONSUMERS,
                        (s) -> (s > 0),
                        Integer.class);
    }

    public double getCpuUtilizationThreshold() {
        return cpuUtilizationThreshold;
    }

    public int getMaximumConsumersToSend() {
        return maxConsumersToSend;
    }

    public static class RCA_CONF_KEY_CONSTANTS {
        public static final String CPU_UTILIZATION_THRESHOLD = "cpu-utilization";
        public static final String TOP_K_CONSUMERS = "top-k-consumers";
    }
}
