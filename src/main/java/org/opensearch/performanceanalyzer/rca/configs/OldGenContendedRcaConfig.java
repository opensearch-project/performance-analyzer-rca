/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.configs;

import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

public class OldGenContendedRcaConfig {
    private static final String CONFIG_NAME = "old-gen-contended-rca-config";
    public static final int DEFAULT_MIN_TOTAL_MEMORY_IN_GB =
            200; // default: Need at least 200GB mem

    private final int minTotalMemoryThresholdInGB;

    public OldGenContendedRcaConfig(final RcaConf conf) {
        this.minTotalMemoryThresholdInGB =
                conf.readRcaConfig(
                        CONFIG_NAME,
                        OldGenContendedRcaKeys.MIN_TOTAL_MEMORY_THRESHOLD_IN_GB.toString(),
                        DEFAULT_MIN_TOTAL_MEMORY_IN_GB,
                        Integer.class);
    }

    enum OldGenContendedRcaKeys {
        MIN_TOTAL_MEMORY_THRESHOLD_IN_GB("min-total-memory-threshold-in-gb");

        private final String value;

        OldGenContendedRcaKeys(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public int getMinTotalMemoryThresholdInGb() {
        return minTotalMemoryThresholdInGB;
    }
}
