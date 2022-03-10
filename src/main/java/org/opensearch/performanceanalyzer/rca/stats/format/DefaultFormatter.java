/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.stats.format;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opensearch.performanceanalyzer.rca.stats.eval.Statistics;
import org.opensearch.performanceanalyzer.rca.stats.eval.impl.vals.AggregateValue;
import org.opensearch.performanceanalyzer.rca.stats.eval.impl.vals.NamedAggregateValue;
import org.opensearch.performanceanalyzer.rca.stats.eval.impl.vals.Value;
import org.opensearch.performanceanalyzer.rca.stats.measurements.MeasurementSet;

public class DefaultFormatter implements Formatter {
    private Map<MeasurementSet, Map<Statistics, List<Value>>> map;
    private long start;
    private long end;

    public DefaultFormatter() {
        this.map = new HashMap<>();
        this.start = 0;
        this.end = 0;
    }

    @Override
    public void formatNamedAggregatedValue(
            MeasurementSet measurementSet, Statistics aggregationType, String name, Number value) {
        map.putIfAbsent(measurementSet, new HashMap<>());
        map.get(measurementSet).putIfAbsent(aggregationType, new ArrayList<>());
        map.get(measurementSet)
                .get(aggregationType)
                .add(new NamedAggregateValue(value, aggregationType, name));
    }

    @Override
    public void formatAggregatedValue(
            MeasurementSet measurementSet, Statistics aggregationType, Number value) {
        map.putIfAbsent(measurementSet, new HashMap<>());
        Value value1;
        if (aggregationType == Statistics.SAMPLE) {
            value1 = new Value(value);
        } else {
            value1 = new AggregateValue(value, aggregationType);
        }

        map.get(measurementSet).put(aggregationType, Collections.singletonList(value1));
    }

    @Override
    public void setStartAndEndTime(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public Map<MeasurementSet, Map<Statistics, List<Value>>> getFormatted() {
        return map;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }
}
