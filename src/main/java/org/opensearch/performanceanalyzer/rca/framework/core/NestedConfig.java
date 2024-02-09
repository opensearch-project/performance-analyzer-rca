/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.core;

import java.util.Map;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NestedConfig {

    private static final Logger LOG = LogManager.getLogger(NestedConfig.class);

    private String key;
    private Map<String, Object> value;

    public NestedConfig(String key, @Nullable Map<String, Object> parentConfig) {
        this.key = key;
        this.value = null;
        if (parentConfig != null) {
            try {
                //noinspection unchecked
                value = (Map<String, Object>) parentConfig.get(key);
            } catch (ClassCastException e) {
                LOG.error(
                        "rca.conf contains invalid value for key: [{}], trace : [{}]",
                        key,
                        e.getMessage());
            }
        }
    }

    public String getKey() {
        return key;
    }

    @Nullable
    public Map<String, Object> getValue() {
        return value;
    }
}
