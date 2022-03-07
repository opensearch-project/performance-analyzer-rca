/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.jvm;


import org.opensearch.performanceanalyzer.rca.framework.core.Config;
import org.opensearch.performanceanalyzer.rca.framework.core.NestedConfig;

/** "level-one-config": { "fielddata-cache-step-size": 1, "shard-request-cache-step-size": 1 } */
public class LevelOneActionBuilderConfig {
    public static final int DEFAULT_FIELD_DATA_CACHE_STEP_SIZE = 1;
    public static final int DEFAULT_SHARD_REQUEST_CACHE_STEP_SIZE = 1;
    private static final String FIELD_DATA_CACHE_STEP_SIZE_CONFIG_NAME =
            "fielddata-cache-step-size";
    private static final String SHARD_REQUEST_CACHE_STEP_SIZE_CONFIG_NAME =
            "shard-request-cache-step-size";
    private Config<Integer> fieldDataCacheStepSize;
    private Config<Integer> shardRequestCacheStepSize;

    public LevelOneActionBuilderConfig(NestedConfig configs) {
        fieldDataCacheStepSize =
                new Config<>(
                        FIELD_DATA_CACHE_STEP_SIZE_CONFIG_NAME,
                        configs.getValue(),
                        DEFAULT_FIELD_DATA_CACHE_STEP_SIZE,
                        (s) -> (s >= 0),
                        Integer.class);
        shardRequestCacheStepSize =
                new Config<>(
                        SHARD_REQUEST_CACHE_STEP_SIZE_CONFIG_NAME,
                        configs.getValue(),
                        DEFAULT_SHARD_REQUEST_CACHE_STEP_SIZE,
                        (s) -> (s >= 0),
                        Integer.class);
    }

    public int fieldDataCacheStepSize() {
        return fieldDataCacheStepSize.getValue();
    }

    public int shardRequestCacheStepSize() {
        return shardRequestCacheStepSize.getValue();
    }
}
