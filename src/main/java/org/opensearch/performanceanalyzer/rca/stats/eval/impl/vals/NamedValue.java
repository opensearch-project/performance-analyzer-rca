/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.stats.eval.impl.vals;


import java.util.Objects;

/**
 * This encapsulates the key and the value. An example will be the name of the RCA node that took
 * the longest and the how long it took.
 */
public abstract class NamedValue extends Value {
    private String name;

    public NamedValue(String key, Number value) {
        super(value);
        this.name = key;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        NamedValue that = (NamedValue) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }

    @Override
    public String toString() {
        return "NamedValue{" + "name='" + name + '\'' + ", value=" + value + '}';
    }
}
