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

public class Sum implements IStatistic<AggregateValue> {
    private AtomicLong sum;
    private boolean empty;

    public Sum() {
        sum = new AtomicLong(0L);
        empty = true;
    }

    @Override
    public Statistics type() {
        return Statistics.SUM;
    }

    @Override
    public void calculate(String key, Number value) {
        sum.addAndGet(value.longValue());
        empty = false;
    }

    @Override
    public List<AggregateValue> get() {
        return Collections.singletonList(new AggregateValue(sum, type()));
    }

    @Override
    public boolean isEmpty() {
        return empty;
    }
}
