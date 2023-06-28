/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.configs;


import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

public class SearchBackPressureRcaConfig {
    public static final String CONFIG_NAME = "search-back-pressure-rca-policy";

    // INTERVAL PERIOD IN SECONDS
    public static final long DEFAULT_EVALUATION_INTERVAL_IN_S = 60;

    // Increase Threshold
    // node max heap usage in last 60 secs is less than 70%
    public static final int DEFAULT_MAX_HEAP_DOWNFLOW_THRESHOLD = 70;

    // cancellationCount due to heap is more than 50% of all task cancellations.
    public static final int DEFAULT_MAX_HEAP_CANCELLATION_THRESHOLD = 50;

    // Decrease Threshold
    // node min heap usage in last 60 secs is more than 80%
    public static final int DEFAULT_MIN_HEAP_OVERFLOW_THRESHOLD = 80;

    // cancellationCount due to heap is more than 30% of all task cancellations
    public static final int DEFAULT_MIN_HEAP_CANCELLATION_THRESHOLD = 30;

    public SearchBackPressureRcaConfig(final RcaConf conf) {}

    // conf file to get Runtime Threshold for SearchBackPressureRCAConfig (TODO)
}
