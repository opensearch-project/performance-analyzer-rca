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
 * serch-back-pressure-policy should be enabled "hour-breach-threshold": 30, // threshold for hourly
 * received unhealthy cluster level rca flow units, if above, then the below thresholds should be
 * modified, "threshold_count": 1, // how many thresholds to be changed, in this case
 * search-heap-threshold, "searchbp-heap-stepsize-in-percentage": 5, } }
 * "searchbp-heap-stepsize-in-percentage" defines the step size to change heap related threshold (in
 * percentage).
 */
public class SearchBackPressurePolicyConfig {
    private static final Logger LOG = LogManager.getLogger(SearchBackPressurePolicyConfig.class);

    // Field Names
    private static final String ENABLED = "enabled";
    private static final String HOUR_BREACH_THRESHOLD = "hour-breach-threshold";
    private static final String THRESHOLD_COUNT = "threshold_count";
    private static final String SEARCHBP_HEAP_STEPSIZE_IN_PERCENTAGE =
            "searchbp-heap-stepsize-in-percentage";

    // Default values
    public static final boolean DEFAULT_ENABLED = true;

    // TO DO: Decide the Default Hour breach threshold
    public static final int DEFAULT_HOUR_BREACH_THRESHOLD = 2;
    public static final int HOUR_MONITOR_WINDOW_SIZE_MINUTES = (int) TimeUnit.HOURS.toMinutes(1);
    public static final int HOUR_MONITOR_BUCKET_SIZE_MINUTES = 1;
    public static final double DEFAULT_SEARCHBP_HEAP_STEPSIZE_IN_PERCENTAGE = 5;

    private Config<Integer> hourBreachThreshold;
    private Config<Boolean> enabled;
    private Config<Double> searchbpHeapStepsizeInPercentage;

    public SearchBackPressurePolicyConfig(NestedConfig config) {
        enabled = new Config<>(ENABLED, config.getValue(), DEFAULT_ENABLED, Boolean.class);
        hourBreachThreshold =
                new Config<>(
                        HOUR_BREACH_THRESHOLD,
                        config.getValue(),
                        DEFAULT_HOUR_BREACH_THRESHOLD,
                        Integer.class);
        LOG.debug(
                "SearchBackPressurePolicyConfig hour breach threshold is: {}",
                hourBreachThreshold.getValue());

        searchbpHeapStepsizeInPercentage =
                new Config<>(
                        SEARCHBP_HEAP_STEPSIZE_IN_PERCENTAGE,
                        config.getValue(),
                        DEFAULT_SEARCHBP_HEAP_STEPSIZE_IN_PERCENTAGE,
                        Double.class);
        LOG.debug(
                "searchbpHeapStepsizeInPercentage is {}",
                searchbpHeapStepsizeInPercentage.getValue());
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
        return HOUR_MONITOR_WINDOW_SIZE_MINUTES;
    }

    public int getHourMonitorBucketSizeMinutes() {
        return HOUR_MONITOR_BUCKET_SIZE_MINUTES;
    }

    public double getSearchbpHeapStepsizeInPercentage() {
        return searchbpHeapStepsizeInPercentage.getValue();
    }
}
