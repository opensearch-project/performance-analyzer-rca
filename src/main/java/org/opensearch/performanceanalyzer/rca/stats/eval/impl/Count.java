/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.stats.eval.impl;


import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.opensearch.performanceanalyzer.rca.stats.eval.Statistics;
import org.opensearch.performanceanalyzer.rca.stats.eval.impl.vals.AggregateValue;

public class Count implements IStatistic<AggregateValue> {
    private AtomicLong counter;
    private boolean empty;

    public Count() {
        counter = new AtomicLong(0L);
        empty = true;
    }

    @Override
    public Statistics type() {
        return Statistics.COUNT;
    }

    @Override
    public void calculate(String key, Number value) {
        counter.incrementAndGet();
        empty = false;
    }

    @Override
    public List<AggregateValue> get() {
        return Collections.singletonList(new AggregateValue(counter, type()));
    }

    @Override
    public boolean isEmpty() {
        return empty;
    }
}
