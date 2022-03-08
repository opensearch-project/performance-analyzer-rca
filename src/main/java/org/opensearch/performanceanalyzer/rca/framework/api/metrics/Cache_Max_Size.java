/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Cache_Max_Size extends Metric {

    public static final String NAME = AllMetrics.CacheConfigValue.CACHE_MAX_SIZE.toString();

    public Cache_Max_Size(long evaluationIntervalSeconds) {
        super(NAME, evaluationIntervalSeconds);
    }
}
