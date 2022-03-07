/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.collectors;


import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.opensearch.performanceanalyzer.metrics.AllMetrics.ShardStatsValue;

class CachedStats {
    private static final Set<String> CACHABLE_VALUES =
            new HashSet<>(
                    Arrays.asList(
                            ShardStatsValue.INDEXING_THROTTLE_TIME.toString(),
                            ShardStatsValue.CACHE_QUERY_HIT.toString(),
                            ShardStatsValue.CACHE_QUERY_MISS.toString(),
                            ShardStatsValue.CACHE_FIELDDATA_EVICTION.toString(),
                            ShardStatsValue.CACHE_REQUEST_HIT.toString(),
                            ShardStatsValue.CACHE_REQUEST_MISS.toString(),
                            ShardStatsValue.CACHE_REQUEST_EVICTION.toString(),
                            ShardStatsValue.REFRESH_EVENT.toString(),
                            ShardStatsValue.REFRESH_TIME.toString(),
                            ShardStatsValue.FLUSH_EVENT.toString(),
                            ShardStatsValue.FLUSH_TIME.toString(),
                            ShardStatsValue.MERGE_EVENT.toString(),
                            ShardStatsValue.MERGE_TIME.toString()));
    private Map<String, Long> cachedValues = new HashMap<>();

    long getValue(String statsName) {
        return cachedValues.getOrDefault(statsName, 0L);
    }

    void putValue(String statsName, long value) {
        cachedValues.put(statsName, value);
    }

    static Set<String> getCachableValues() {
        return CACHABLE_VALUES;
    }
}
