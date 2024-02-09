/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.configs;

import org.opensearch.performanceanalyzer.rca.framework.core.Config;
import org.opensearch.performanceanalyzer.rca.framework.core.NestedConfig;

/** "workload-type": { "prefer-ingest": true, "prefer-search": false } */
public class WorkLoadTypeConfig {
    private static final String INGEST_CONFIG = "prefer-ingest";
    private static final String SEARCH_CONFIG = "prefer-search";
    public static final boolean DEFAULT_PREFER_INGEST = false;
    public static final boolean DEFAULT_PREFER_SEARCH = false;
    private Config<Boolean> preferIngest;
    private Config<Boolean> preferSearch;

    public WorkLoadTypeConfig(NestedConfig configs) {
        preferIngest =
                new Config<>(
                        INGEST_CONFIG, configs.getValue(), DEFAULT_PREFER_INGEST, Boolean.class);
        preferSearch =
                new Config<>(
                        SEARCH_CONFIG, configs.getValue(), DEFAULT_PREFER_SEARCH, Boolean.class);
    }

    public boolean preferIngest() {
        return preferIngest.getValue();
    }

    public boolean preferSearch() {
        return preferSearch.getValue();
    }
}
