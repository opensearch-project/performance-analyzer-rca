/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.jvm;

import org.opensearch.performanceanalyzer.rca.framework.core.Config;
import org.opensearch.performanceanalyzer.rca.framework.core.NestedConfig;

/** "level-three-config": { "write-queue-step-size": 2, "search-queue-step-size":2 } */
public class LevelThreeActionBuilderConfig {
    public static final int DEFAULT_WRITE_QUEUE_STEP_SIZE = 2;
    public static final int DEFAULT_SEARCH_QUEUE_STEP_SIZE = 2;
    private static final String WRITE_QUEUE_STEP_SIZE_CONFIG_NAME = "write-queue-step-size";
    private static final String SEARCH_QUEUE_STEP_SIZE_CONFIG_NAME = "search-queue-step-size";
    private Config<Integer> writeQueueStepSize;
    private Config<Integer> searchQueueStepSize;

    public LevelThreeActionBuilderConfig(NestedConfig configs) {
        writeQueueStepSize =
                new Config<>(
                        WRITE_QUEUE_STEP_SIZE_CONFIG_NAME,
                        configs.getValue(),
                        DEFAULT_WRITE_QUEUE_STEP_SIZE,
                        (s) -> (s >= 0),
                        Integer.class);
        searchQueueStepSize =
                new Config<>(
                        SEARCH_QUEUE_STEP_SIZE_CONFIG_NAME,
                        configs.getValue(),
                        DEFAULT_SEARCH_QUEUE_STEP_SIZE,
                        (s) -> (s >= 0),
                        Integer.class);
    }

    public int writeQueueStepSize() {
        return writeQueueStepSize.getValue();
    }

    public int searchQueueStepSize() {
        return searchQueueStepSize.getValue();
    }
}
