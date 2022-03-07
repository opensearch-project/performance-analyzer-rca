/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.stats.eval.impl.vals;


import java.util.Objects;
import org.opensearch.performanceanalyzer.rca.stats.eval.Statistics;
import org.opensearch.performanceanalyzer.rca.stats.format.Formatter;
import org.opensearch.performanceanalyzer.rca.stats.measurements.MeasurementSet;

public class NamedAggregateValue extends AggregateValue {
    private String name;

    public NamedAggregateValue(Number value, Statistics type, String name) {
        super(value, type);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public void format(Formatter formatter, MeasurementSet measurementSet, Statistics stats) {
        formatter.formatNamedAggregatedValue(
                measurementSet, getAggregationType(), getName(), getValue());
    }

    public void update(Number value) {
        this.value = value;
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
        NamedAggregateValue that = (NamedAggregateValue) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }

    @Override
    public String toString() {
        return "NamedAggregateValue{"
                + "name='"
                + name
                + '\''
                + ", aggr='"
                + getAggregationType()
                + '\''
                + ", value="
                + value
                + '}';
    }
}
