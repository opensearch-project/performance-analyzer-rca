/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metricsdb;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class Dimensions {
    // Dimension is a key, value
    private Map<String, String> dimensions;

    public Dimensions() {
        this.dimensions = new HashMap<>();
    }

    public void put(String key, String value) {
        this.dimensions.put(key, value);
    }

    public String get(String key) {
        return this.dimensions.get(key);
    }

    public Map<Field<String>, String> getFieldMap() {
        Map<Field<String>, String> fieldMap = new HashMap<Field<String>, String>();
        for (Map.Entry<String, String> entry : dimensions.entrySet()) {
            fieldMap.put(DSL.field(DSL.name(entry.getKey()), String.class), entry.getValue());
        }
        return fieldMap;
    }

    public Set<String> getDimensionNames() {
        return this.dimensions.keySet();
    }
}
