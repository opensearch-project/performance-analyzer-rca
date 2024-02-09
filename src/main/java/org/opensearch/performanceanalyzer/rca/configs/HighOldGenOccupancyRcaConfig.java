/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.configs;

import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

public class HighOldGenOccupancyRcaConfig {

    private static final String CONFIG_NAME = "high-old-gen-occupancy-config";
    public static final long DEFAULT_UTILIZATION = 75;
    public static final long DEFAULT_EVALUATION_INTERVAL_IN_S = 60;
    private final Long heapUtilizationThreshold;

    private final long evaluationIntervalInS;

    public HighOldGenOccupancyRcaConfig(final RcaConf conf) {
        this.evaluationIntervalInS =
                conf.readRcaConfig(
                        CONFIG_NAME,
                        HighOldGenOccupancyRcaConfigKeys.EVALUATION_INTERVAL_IN_S.toString(),
                        DEFAULT_EVALUATION_INTERVAL_IN_S,
                        Long.class);

        this.heapUtilizationThreshold =
                conf.readRcaConfig(
                        CONFIG_NAME,
                        HighOldGenOccupancyRcaConfigKeys.HEAP_UTILIZATION_THRESHOLD.toString(),
                        DEFAULT_UTILIZATION,
                        Long.class);
    }

    enum HighOldGenOccupancyRcaConfigKeys {
        HEAP_UTILIZATION_THRESHOLD("heap-utilization-threshold"),
        EVALUATION_INTERVAL_IN_S("eval-interval-in-s");

        private final String value;

        HighOldGenOccupancyRcaConfigKeys(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    public Long getHeapUtilizationThreshold() {
        return heapUtilizationThreshold;
    }

    public long getEvaluationIntervalInS() {
        return evaluationIntervalInS;
    }
}
