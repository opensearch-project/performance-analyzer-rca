/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.stats.eval.impl;


import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.rca.stats.eval.Statistics;
import org.opensearch.performanceanalyzer.rca.stats.eval.impl.vals.NamedAggregateValue;

public class NamedCounter implements IStatistic<NamedAggregateValue> {

    private static final Logger LOG = LogManager.getLogger(NamedCounter.class);
    private boolean empty;
    private Map<String, NamedAggregateValue> counters;

    public NamedCounter() {
        counters = new ConcurrentHashMap<>();
        empty = true;
    }

    @Override
    public Statistics type() {
        return Statistics.NAMED_COUNTERS;
    }

    @Override
    public void calculate(String key, Number value) {
        synchronized (this) {
            NamedAggregateValue mapValue =
                    counters.getOrDefault(
                            key, new NamedAggregateValue(0L, Statistics.NAMED_COUNTERS, key));
            try {
                Number numb = mapValue.getValue();
                long number = mapValue.getValue().longValue();
                long newNumber = number + 1;
                mapValue.update(newNumber);
                counters.put(key, mapValue);
                empty = false;
            } catch (Exception ex) {
                LOG.error("Caught an exception while calculating the counter value", ex);
            }
        }
    }

    @Override
    public Collection<NamedAggregateValue> get() {
        return counters.values();
    }

    @Override
    public boolean isEmpty() {
        return empty;
    }
}
