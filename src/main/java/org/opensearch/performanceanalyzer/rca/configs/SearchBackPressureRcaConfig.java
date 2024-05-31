/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.configs;

import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

public class SearchBackPressureRcaConfig {
    public static final String CONFIG_NAME = "search-back-pressure-rca";

    /* Metadata fields for thresholds */
    public static final String INCREASE_THRESHOLD_BY_JVM_STR = "increase_jvm";
    public static final String DECREASE_THRESHOLD_BY_JVM_STR = "decrease_jvm";

    public static final int SLIDING_WINDOW_SIZE_IN_MINS = 1;

    // Interval period in seconds
    public static final long DEFAULT_EVALUATION_INTERVAL_IN_S = 60;

    /* interval period to call operate() */
    public static final long EVAL_INTERVAL_IN_S = 5;

    /* Increase Threshold */
    // node max heap usage in last 60 secs is less than 70%
    public static final int DEFAULT_MAX_HEAP_INCREASE_THRESHOLD_PERCENT = 80;
    private Integer maxHeapIncreasePercentageThreshold;

    // cancellation percent due to heap is more than 5% of all task completions at shard level
    // (Taking 3 because we don't cancel more than 10% of all completions at any time)
    // Basically this threshold tell that we are overcancelling the shard level tasks given max heap
    // from last rca eval period is still
    // below or equal to DEFAULT_MAX_HEAP_INCREASE_THRESHOLD
    public static final int DEFAULT_SHARD_MAX_HEAP_CANCELLATION_THRESHOLD_PERCENT = 5;
    private Integer maxShardHeapCancellationPercentageThreshold;

    //  cancellation percent due to heap is more than 5% of all task completions in
    // SearchTask(co-ordinator) level (Taking 3 because we don't cancel more than 10% of all
    // completions at any time)
    // Basically this threshold tell that we are overcancelling the co-ordinator level tasks
    public static final int DEFAULT_TASK_MAX_HEAP_CANCELLATION_THRESHOLD_PERCENT = 5;
    private Integer maxTaskHeapCancellationPercentageThreshold;

    /* Decrease Threshold */
    // node min heap usage in last 60 secs is more than 80%
    public static final int DEFAULT_MIN_HEAP_DECREASE_THRESHOLD_PERCENT = 90;
    private Integer minHeapDecreasePercentageThreshold;

    // cancellationCount due to heap is less than 3% of all task completions in shard level
    // Basically this threshold tell that we are under cancelling the shard level tasks given min
    // heap from last rca eval period is still
    // above or equal to DEFAULT_MIN_HEAP_DECREASE_THRESHOLD
    public static final int DEFAULT_SHARD_MIN_HEAP_CANCELLATION_THRESHOLD_PERCENT = 3;
    private Integer minShardHeapCancellationPercentageThreshold;

    // cancellationCount due to heap is less than 3% of all task completions in task level
    // Basically this threshold tell that we are under cancelling the coordinator level tasks given
    // min heap from last rca eval period is still
    // above or equal to DEFAULT_MIN_HEAP_DECREASE_THRESHOLD
    public static final int DEFAULT_TASK_MIN_HEAP_CANCELLATION_THRESHOLD_PERCENT = 3;
    private Integer minTaskHeapCancellationPercentageThreshold;

    public SearchBackPressureRcaConfig(final RcaConf conf) {
        // (s) -> s > 0 is the validator, if validated, fields from conf file will be returned,
        // else, default value gets returned
        maxHeapIncreasePercentageThreshold =
                conf.readRcaConfig(
                        CONFIG_NAME,
                        SearchBackPressureRcaConfigKeys.MAX_HEAP_USAGE_INCREASE_FIELD.toString(),
                        DEFAULT_MAX_HEAP_INCREASE_THRESHOLD_PERCENT,
                        (s) -> s >= 0 && s <= 100,
                        Integer.class);
        maxShardHeapCancellationPercentageThreshold =
                conf.readRcaConfig(
                        CONFIG_NAME,
                        SearchBackPressureRcaConfigKeys.MAX_SHARD_HEAP_CANCELLATION_PERCENTAGE_FIELD
                                .toString(),
                        DEFAULT_SHARD_MAX_HEAP_CANCELLATION_THRESHOLD_PERCENT,
                        (s) -> s >= 0 && s <= 100,
                        Integer.class);
        maxTaskHeapCancellationPercentageThreshold =
                conf.readRcaConfig(
                        CONFIG_NAME,
                        SearchBackPressureRcaConfigKeys.MAX_TASK_HEAP_CANCELLATION_PERCENTAGE_FIELD
                                .toString(),
                        DEFAULT_TASK_MAX_HEAP_CANCELLATION_THRESHOLD_PERCENT,
                        (s) -> s >= 0 && s <= 100,
                        Integer.class);
        minHeapDecreasePercentageThreshold =
                conf.readRcaConfig(
                        CONFIG_NAME,
                        SearchBackPressureRcaConfigKeys.MAX_HEAP_USAGE_DECREASE_FIELD.toString(),
                        DEFAULT_MIN_HEAP_DECREASE_THRESHOLD_PERCENT,
                        (s) -> s >= 0 && s <= 100,
                        Integer.class);
        minShardHeapCancellationPercentageThreshold =
                conf.readRcaConfig(
                        CONFIG_NAME,
                        SearchBackPressureRcaConfigKeys.MIN_SHARD_HEAP_CANCELLATION_PERCENTAGE_FIELD
                                .toString(),
                        DEFAULT_SHARD_MIN_HEAP_CANCELLATION_THRESHOLD_PERCENT,
                        (s) -> s >= 0 && s <= 100,
                        Integer.class);
        minTaskHeapCancellationPercentageThreshold =
                conf.readRcaConfig(
                        CONFIG_NAME,
                        SearchBackPressureRcaConfigKeys.MIN_TASK_HEAP_CANCELLATION_PERCENTAGE_FIELD
                                .toString(),
                        DEFAULT_TASK_MIN_HEAP_CANCELLATION_THRESHOLD_PERCENT,
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
    public enum SearchBackPressureRcaConfigKeys {
        MAX_HEAP_USAGE_INCREASE_FIELD("max-heap-usage-increase"),
        MAX_SHARD_HEAP_CANCELLATION_PERCENTAGE_FIELD("max-shard-heap-cancellation-percentage"),
        MAX_TASK_HEAP_CANCELLATION_PERCENTAGE_FIELD("max-task-heap-cancellation-percentage"),
        MAX_HEAP_USAGE_DECREASE_FIELD("max-heap-usage-decrease"),
        MIN_SHARD_HEAP_CANCELLATION_PERCENTAGE_FIELD("min-shard-heap-cancellation-percentage"),
        MIN_TASK_HEAP_CANCELLATION_PERCENTAGE_FIELD("min-task-heap-cancellation-percentage");

        private final String value;

        SearchBackPressureRcaConfigKeys(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
