/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.searchbackpressure;


import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.searchbackpressure.SearchBackPressurePolicy;
import org.opensearch.performanceanalyzer.rca.framework.core.Config;
import org.opensearch.performanceanalyzer.rca.framework.core.NestedConfig;

/**
 * Configures various thresholds for the {@link SearchBackPressurePolicy}
 *
 * <p>The config follows the format below "decider-config-settings": {
 * "search-back-pressure-policy-config": { "enabled": true, // whether the
 * serch-back-pressure-policy should be enabled "hour-threshold": 30, // threshold for hourly
 * received unhealthy cluster level rca flow units, if above, then the below thresholds should be
 * modified "threshold_count": 2, // how many thresholds to be changed, in this case
 * search-heap-threshold and search-task-heap-threshold "search_task_heap_stepsize_in_percentage":
 * 5, "search_task_stepsize_in_percentage": 0.5" } } Explanation of thresholds that are being
 * configured and modified based on current RCA flowunits: search_task_heap_stepsize_in_percentage:
 * Defines the step size to change heap usage threshold (in percentage). for the sum of heap usages
 * across all search tasks before in-flight cancellation is applied.
 * search_task_stepsize_in_percentage: Defines the step size to change heap usage threshold (in
 * percentage) for an individual task before it is considered for cancellation.
 */
public class SearchBackPressurePolicyConfig {
    private static final Logger LOG = LogManager.getLogger(SearchBackPressurePolicyConfig.class);

    // Field Names
    private static final String ENABLED = "enabled";
    private static final String HOUR_BREACH_THRESHOLD = "hour-breach-threshold";
    private static final String THRESHOLD_COUNT = "threshold_count";
    private static final String HOUR_MONITOR_WINDOW_SIZE_MINUTES =
            "hour-monitor-window-size-minutes";
    private static final String HOUR_MONITOR_BUCKET_SIZE_MINUTES =
            "hour-monitor-bucket-size-minutes";

    // Default values
    public static final boolean DEFAULT_ENABLED = true;
    // TO DO Decide the Defauilt Hour breach threshold
    public static final int DEFAULT_HOUR_BREACH_THRESHOLD = 2;
    public static final int DEFAULT_HOUR_MONITOR_WINDOW_SIZE_MINUTES =
            (int) TimeUnit.HOURS.toMinutes(1);
    public static final int DEFAULT_HOUR_MONITOR_BUCKET_SIZE_MINUTES = 5;

    private Config<Integer> hourBreachThreshold;
    private Config<Boolean> enabled;
    private Config<Integer> hourMonitorWindowSizeMinutes;
    private Config<Integer> hourMonitorBucketSizeMinutes;

    public SearchBackPressurePolicyConfig(NestedConfig config) {
        enabled = new Config<>(ENABLED, config.getValue(), DEFAULT_ENABLED, Boolean.class);
        hourBreachThreshold =
                new Config<>(
                        HOUR_BREACH_THRESHOLD,
                        config.getValue(),
                        DEFAULT_HOUR_BREACH_THRESHOLD,
                        Integer.class);
        LOG.info(
                "SearchBackPressurePolicyConfig hour breach threshold is: {}",
                hourBreachThreshold.getValue());
        hourMonitorWindowSizeMinutes =
                new Config<>(
                        HOUR_MONITOR_WINDOW_SIZE_MINUTES,
                        config.getValue(),
                        DEFAULT_HOUR_MONITOR_WINDOW_SIZE_MINUTES,
                        Integer.class);
        LOG.info("hourMonitorWindowSizeMinutes is: {}", hourMonitorWindowSizeMinutes.getValue());
        hourMonitorBucketSizeMinutes =
                new Config<>(
                        HOUR_MONITOR_BUCKET_SIZE_MINUTES,
                        config.getValue(),
                        DEFAULT_HOUR_MONITOR_BUCKET_SIZE_MINUTES,
                        Integer.class);
    }

    /**
     * Whether or not to enable the policy. A disabled policy will not emit any actions.
     *
     * @return Whether or not to enable the policy
     */
    public boolean isEnabled() {
        return enabled.getValue();
    }

    public int getHourBreachThreshold() {
        return hourBreachThreshold.getValue();
    }

    public int getHourMonitorWindowSizeMinutes() {
        return hourMonitorWindowSizeMinutes.getValue();
    }

    public int getHourMonitorBucketSizeMinutes() {
        return hourMonitorBucketSizeMinutes.getValue();
    }
}
