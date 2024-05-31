/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.configs;

import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

public class HeapSizeIncreasePolicyConfig {

    private static final String POLICY_NAME = "heap-size-increase-policy";
    public static final int DEFAULT_UNHEALTHY_NODE_PERCENTAGE = 50;
    public static final int DEFAULT_MIN_UNHEALTHY_MINUTES = 2 * 24 * 60;
    private static final int DEFAULT_DAY_BREACH_THRESHOLD = 8;
    private static final int DEFAULT_WEEK_BREACH_THRESHOLD = 3;
    private final int unhealthyNodePercentage;
    private final int dayBreachThreshold;
    private final int weekBreachThreshold;

    public HeapSizeIncreasePolicyConfig(final RcaConf rcaConf) {
        this.unhealthyNodePercentage =
                rcaConf.readRcaConfig(
                        POLICY_NAME,
                        HeapSizeIncreasePolicyKeys.UNHEALTHY_NODE_PERCENTAGE_KEY.toString(),
                        DEFAULT_UNHEALTHY_NODE_PERCENTAGE,
                        Integer.class);
        this.dayBreachThreshold =
                rcaConf.readRcaConfig(
                        POLICY_NAME,
                        HeapSizeIncreasePolicyKeys.DAY_BREACH_THRESHOLD_KEY.toString(),
                        DEFAULT_DAY_BREACH_THRESHOLD,
                        Integer.class);
        this.weekBreachThreshold =
                rcaConf.readRcaConfig(
                        POLICY_NAME,
                        HeapSizeIncreasePolicyKeys.WEEK_BREACH_THRESHOLD_KEY.toString(),
                        DEFAULT_WEEK_BREACH_THRESHOLD,
                        Integer.class);
    }

    enum HeapSizeIncreasePolicyKeys {
        UNHEALTHY_NODE_PERCENTAGE_KEY("unhealthy-node-percentage"),
        DAY_BREACH_THRESHOLD_KEY("day-breach-threshold"),
        WEEK_BREACH_THRESHOLD_KEY("week-breach-threshold");

        private final String value;

        HeapSizeIncreasePolicyKeys(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public int getUnhealthyNodePercentage() {
        return unhealthyNodePercentage;
    }

    public int getDayBreachThreshold() {
        return dayBreachThreshold;
    }

    public int getWeekBreachThreshold() {
        return weekBreachThreshold;
    }
}
