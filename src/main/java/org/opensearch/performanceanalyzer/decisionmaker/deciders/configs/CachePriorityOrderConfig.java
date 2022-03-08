/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.configs;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.opensearch.performanceanalyzer.rca.framework.core.Config;
import org.opensearch.performanceanalyzer.rca.framework.core.NestedConfig;

/** "cache-type": { "priority-order": ["fielddata-cache", "shard-request-cache"] } */
public class CachePriorityOrderConfig {
    private static final String PRIORITY_ORDER_CONFIG_NAME = "priority-order";
    private static String FIELDDATA_CACHE = "fielddata-cache";
    private static String SHARD_REQUEST_CACHE = "shard-request-cache";
    public static final List<String> DEFAULT_PRIORITY_ORDER =
            Collections.unmodifiableList(Arrays.asList(FIELDDATA_CACHE, SHARD_REQUEST_CACHE));
    private Config<List<String>> priorityOrder;

    public CachePriorityOrderConfig(NestedConfig configs) {
        priorityOrder =
                new Config(
                        PRIORITY_ORDER_CONFIG_NAME,
                        configs.getValue(),
                        DEFAULT_PRIORITY_ORDER,
                        List.class);
    }

    public List<String> getPriorityOrder() {
        return priorityOrder.getValue();
    }
}
