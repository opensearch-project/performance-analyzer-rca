/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.core;

import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Config<T> {

    private static final Logger LOG = LogManager.getLogger(Config.class);

    private String key;
    private T value;

    public Config(
            String key,
            @Nullable Map<String, Object> parentConfig,
            T defaultValue,
            Class<? extends T> clazz) {
        this(key, parentConfig, defaultValue, (s) -> true, clazz);
    }

    public Config(
            String key,
            @Nullable Map<String, Object> parentConfig,
            T defaultValue,
            Predicate<T> validator,
            Class<? extends T> clazz) {
        this.key = key;
        this.value = defaultValue;
        if (parentConfig != null) {
            try {
                T configValue = clazz.cast(parentConfig.getOrDefault(key, defaultValue));
                if (!validator.test(configValue)) {
                    LOG.error(
                            "Config value: [{}] provided for key: [{}] is invalid",
                            configValue,
                            key);
                } else {
                    value = configValue;
                }
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

    public T getValue() {
        return value;
    }
}
