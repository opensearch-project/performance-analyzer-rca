/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.configs;


import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

public class SearchBackPressureRcaConfig {
    public static final String CONFIG_NAME = "search-back-pressure-rca-policy";

    /* Metadata fields for thresholds */
    public static final String INCREASE_THRESHOLD_BY_JVM_STR = "increase_jvm";
    public static final String DECREASE_THRESHOLD_BY_JVM_STR = "decrease_jvm";

    public static final int SLIDING_WINDOW_SIZE_IN_MINS = 1;

    // Interval period in seconds
    public static final long DEFAULT_EVALUATION_INTERVAL_IN_S = 60;

    /* interval period to call operate() */
    public static final long EVAL_INTERVAL_IN_S = 2;

    /* Increase Threshold */
    // node max heap usage in last 60 secs is less than 70%
    public static final int DEFAULT_MAX_HEAP_INCREASE_THRESHOLD = 70;
    private Integer maxHeapIncreasePercentageThreshold;

    // cancellationCount due to heap is more than 50% of all task cancellations in shard level
    public static final int DEFAULT_SHARD_MAX_HEAP_CANCELLATION_THRESHOLD = 50;
    private Integer maxShardHeapCancellationPercentageThreshold;

    // cancellationCount due to heap is more than 50% of all task cancellations in task level
    public static final int DEFAULT_TASK_MAX_HEAP_CANCELLATION_THRESHOLD = 50;
    private Integer maxTaskHeapCancellationPercentageThreshold;

    /* Decrease Threshold */
    // node min heap usage in last 60 secs is more than 80%
    public static final int DEFAULT_MIN_HEAP_DECREASE_THRESHOLD = 80;
    private Integer minHeapDecreasePercentageThreshold;

    // cancellationCount due to heap is less than 30% of all task cancellations in shard level
    public static final int DEFAULT_SHARD_MIN_HEAP_CANCELLATION_THRESHOLD = 30;
    private Integer minShardHeapCancellationPercentageThreshold;

    // cancellationCount due to heap is less than 30% of all task cancellations in task level
    public static final int DEFAULT_TASK_MIN_HEAP_CANCELLATION_THRESHOLD = 30;
    private Integer minTaskHeapCancellationPercentageThreshold;

    public SearchBackPressureRcaConfig(final RcaConf conf) {
        // (s) -> s > 0 is the validator, if validated, fields from conf file will be returned,
        // else, default value gets returned
        maxHeapIncreasePercentageThreshold =
                conf.readRcaConfig(
                        CONFIG_NAME,
                        RCA_CONF_KEY_CONSTANTS.MAX_HEAP_USAGE_INCREASE_FIELD,
                        DEFAULT_MAX_HEAP_INCREASE_THRESHOLD,
                        (s) -> s >= 0 && s <= 100,
                        Integer.class);
        maxShardHeapCancellationPercentageThreshold =
                conf.readRcaConfig(
                        CONFIG_NAME,
                        RCA_CONF_KEY_CONSTANTS.MAX_SHARD_HEAP_CANCELLATION_PERCENTAGE_FIELD,
                        DEFAULT_SHARD_MAX_HEAP_CANCELLATION_THRESHOLD,
                        (s) -> s >= 0 && s <= 100,
                        Integer.class);
        maxTaskHeapCancellationPercentageThreshold =
                conf.readRcaConfig(
                        CONFIG_NAME,
                        RCA_CONF_KEY_CONSTANTS.MAX_TASK_HEAP_CANCELLATION_PERCENTAGE_FIELD,
                        DEFAULT_TASK_MAX_HEAP_CANCELLATION_THRESHOLD,
                        (s) -> s >= 0 && s <= 100,
                        Integer.class);
        minHeapDecreasePercentageThreshold =
                conf.readRcaConfig(
                        CONFIG_NAME,
                        RCA_CONF_KEY_CONSTANTS.MAX_HEAP_USAGE_DECREASE_FIELD,
                        DEFAULT_MIN_HEAP_DECREASE_THRESHOLD,
                        (s) -> s >= 0 && s <= 100,
                        Integer.class);
        minShardHeapCancellationPercentageThreshold =
                conf.readRcaConfig(
                        CONFIG_NAME,
                        RCA_CONF_KEY_CONSTANTS.MIN_SHARD_HEAP_CANCELLATION_PERCENTAGE_FIELD,
                        DEFAULT_SHARD_MIN_HEAP_CANCELLATION_THRESHOLD,
                        (s) -> s >= 0 && s <= 100,
                        Integer.class);
        minTaskHeapCancellationPercentageThreshold =
                conf.readRcaConfig(
                        CONFIG_NAME,
                        RCA_CONF_KEY_CONSTANTS.MIN_TASK_HEAP_CANCELLATION_PERCENTAGE_FIELD,
                        DEFAULT_TASK_MIN_HEAP_CANCELLATION_THRESHOLD,
                        (s) -> s >= 0 && s <= 100,
                        Integer.class);
    }

    // Getters for private field
    public Integer getMaxHeapIncreasePercentageThreshold() {
        return maxHeapIncreasePercentageThreshold;
    }

    public Integer getMaxShardHeapCancellationPercentageThreshold() {
        return maxShardHeapCancellationPercentageThreshold;
    }

    public Integer getMaxTaskHeapCancellationPercentageThreshold() {
        return maxTaskHeapCancellationPercentageThreshold;
    }

    public Integer getMinHeapDecreasePercentageThreshold() {
        return minHeapDecreasePercentageThreshold;
    }

    public Integer getMinShardHeapCancellationPercentageThreshold() {
        return minShardHeapCancellationPercentageThreshold;
    }

    public Integer getMinTaskHeapCancellationPercentageThreshold() {
        return minTaskHeapCancellationPercentageThreshold;
    }

    // name for the configuration field
    public static class RCA_CONF_KEY_CONSTANTS {
        public static final String MAX_HEAP_USAGE_INCREASE_FIELD = "max-heap-usage-increase";
        public static final String MAX_SHARD_HEAP_CANCELLATION_PERCENTAGE_FIELD =
                "max-shard-heap-cancellation-percentage";
        public static final String MAX_TASK_HEAP_CANCELLATION_PERCENTAGE_FIELD =
                "max-task-heap-cancellation-percentage";
        public static final String MAX_HEAP_USAGE_DECREASE_FIELD = "max-heap-usage-decrease";
        public static final String MIN_SHARD_HEAP_CANCELLATION_PERCENTAGE_FIELD =
                "min-shard-heap-cancellation-percentage";
        public static final String MIN_TASK_HEAP_CANCELLATION_PERCENTAGE_FIELD =
                "min-task-heap-cancellation-percentage";
    }
}
