/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.configs;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

public class SearchBackPressureRcaConfig {
    public static final String CONFIG_NAME = "search-back-pressure-rca-policy";

    // Interval period in seconds
    public static final long DEFAULT_EVALUATION_INTERVAL_IN_S = 60;

    // Increase Threshold
    // node max heap usage in last 60 secs is less than 70%
    public static final int DEFAULT_MAX_HEAP_INCREASE_THRESHOLD = 70;
    private Integer maxHeapIncreasePercentageThreshold;

    // cancellationCount due to heap is more than 50% of all task cancellations.
    public static final int DEFAULT_MAX_HEAP_CANCELLATION_THRESHOLD = 50;
    private Integer maxHeapCancellationPercentageThreshold;

    // Decrease Threshold
    // node min heap usage in last 60 secs is more than 80%
    public static final int DEFAULT_MAX_HEAP_DECREASE_THRESHOLD = 80;
    private Integer maxHeapDecreasePercentageThreshold;

    // cancellationCount due to heap is less than 30% of all task cancellations
    public static final int DEFAULT_MIN_HEAP_CANCELLATION_THRESHOLD = 30;
    private Integer minHeapCancellationPercentageThreshold;

    public SearchBackPressureRcaConfig(final RcaConf conf) {
        // (s) -> s > 0 is the validator, if validated, fields from conf file will be returned, else, default value gets returned
        maxHeapIncreasePercentageThreshold =
                conf.readRcaConfig(
                        CONFIG_NAME,
                        RCA_CONF_KEY_CONSTANTS.MAX_HEAP_USAGE_INCREASE_FIELD,
                        DEFAULT_MAX_HEAP_INCREASE_THRESHOLD,
                        (s) -> s >= 0 && s <= 100,
                        Integer.class);
        maxHeapCancellationPercentageThreshold =
                conf.readRcaConfig(
                        CONFIG_NAME,
                        RCA_CONF_KEY_CONSTANTS.MAX_HEAP_CANCELLATION_PERCENTAGE_FIELD,
                        DEFAULT_MAX_HEAP_CANCELLATION_THRESHOLD,
                        (s) -> s >= 0 && s <= 100,
                        Integer.class);
        maxHeapDecreasePercentageThreshold =
                conf.readRcaConfig(
                        CONFIG_NAME,
                        RCA_CONF_KEY_CONSTANTS.MAX_HEAP_USAGE_DECREASE_FIELD,
                        DEFAULT_MAX_HEAP_DECREASE_THRESHOLD,
                        (s) -> s >= 0 && s <= 100,
                        Integer.class);
        minHeapCancellationPercentageThreshold =
                conf.readRcaConfig(
                        CONFIG_NAME,
                        RCA_CONF_KEY_CONSTANTS.MIN_HEAP_CANCELLATION_PERCENTAGE_FIELD,
                        DEFAULT_MIN_HEAP_CANCELLATION_THRESHOLD,
                        (s) -> s >= 0 && s <= 100,
                        Integer.class);
    }

    // Getters for private field
    public int getMaxHeapIncreasePercentageThreshold() {
        return maxHeapIncreasePercentageThreshold;
    }

    public int getMaxHeapCancellationPercentageThreshold() {
        return maxHeapCancellationPercentageThreshold;
    }

    public int getMaxHeapDecreasePercentageThreshold() {
        return maxHeapDecreasePercentageThreshold;
    }

    public int getMinHeapCancellationPercentageThreshold() {
        return minHeapCancellationPercentageThreshold;
    }

    // name for the configuration field
    public static class RCA_CONF_KEY_CONSTANTS {
        public static final String MAX_HEAP_USAGE_INCREASE_FIELD = "max-heap-usage-increase";
        public static final String MAX_HEAP_CANCELLATION_PERCENTAGE_FIELD = "max-heap-cancellation-percentage";
        public static final String MAX_HEAP_USAGE_DECREASE_FIELD = "max-heap-usage-decrease";
        public static final String MIN_HEAP_CANCELLATION_PERCENTAGE_FIELD = "min-heap-cancellation-percentage";
    }
}
 