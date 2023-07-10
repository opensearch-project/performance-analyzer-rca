/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.searchbackpressure;


import org.opensearch.performanceanalyzer.decisionmaker.deciders.searchbackpressure.SearchBackPressurePolicy;

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
public class SearchBackPressurePolicyConfig {}
