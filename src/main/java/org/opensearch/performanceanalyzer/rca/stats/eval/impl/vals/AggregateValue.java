/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.stats.eval.impl.vals;


import java.util.Objects;
import org.opensearch.performanceanalyzer.rca.stats.eval.Statistics;
import org.opensearch.performanceanalyzer.rca.stats.format.Formatter;
import org.opensearch.performanceanalyzer.rca.stats.measurements.MeasurementSet;

public class AggregateValue extends Value {
    private Statistics aggregationType;

    public AggregateValue(Number value, Statistics type) {
        super(value);
        this.aggregationType = type;
    }

    public void format(Formatter formatter, MeasurementSet measurementSet, Statistics stats) {
        formatter.formatAggregatedValue(measurementSet, stats, value);
    }

    public Statistics getAggregationType() {
        return aggregationType;
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
        AggregateValue that = (AggregateValue) o;
        return aggregationType == that.aggregationType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), aggregationType);
    }

    @Override
    public String toString() {
        return "AggregateValue{" + "aggregationType=" + aggregationType + ", value=" + value + '}';
    }
}
