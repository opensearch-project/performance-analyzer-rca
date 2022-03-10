/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.stats.eval.impl;


import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import org.opensearch.performanceanalyzer.rca.stats.eval.Statistics;
import org.opensearch.performanceanalyzer.rca.stats.eval.impl.vals.AggregateValue;

public class Mean implements IStatistic<AggregateValue> {
    private BigInteger sum;
    private long count;

    private boolean empty;

    public Mean() {
        this.sum = BigInteger.ZERO;
        this.count = 0;
        this.empty = true;
    }

    @Override
    public Statistics type() {
        return Statistics.MEAN;
    }

    @Override
    public void calculate(String key, Number value) {
        synchronized (this) {
            BigInteger bdValue = BigInteger.valueOf(value.longValue());
            sum = sum.add(bdValue);
            count += 1;
        }
        empty = false;
    }

    @Override
    public List<AggregateValue> get() {
        double ret = 0.0;
        synchronized (this) {
            if (count != 0) {
                ret = sum.doubleValue() / count;
            }
        }
        return Collections.singletonList(new AggregateValue(ret, type()));
    }

    @Override
    public boolean isEmpty() {
        return empty;
    }
}
