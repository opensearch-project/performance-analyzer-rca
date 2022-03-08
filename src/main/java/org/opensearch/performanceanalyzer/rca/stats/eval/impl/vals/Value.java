/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.stats.eval.impl.vals;


import java.util.Objects;
import org.opensearch.performanceanalyzer.rca.stats.eval.Statistics;
import org.opensearch.performanceanalyzer.rca.stats.format.Formatter;
import org.opensearch.performanceanalyzer.rca.stats.measurements.MeasurementSet;

public class Value {
    protected Number value;

    public Value(Number value) {
        this.value = value;
    }

    public Number getValue() {
        return value;
    }

    public void format(Formatter formatter, MeasurementSet measurementSet, Statistics stats) {
        formatter.formatAggregatedValue(measurementSet, stats, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Value value1 = (Value) o;
        return Objects.equals(value.longValue(), value1.getValue().longValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "Value{" + "value=" + value + '}';
    }
}
