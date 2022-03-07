/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.stats.eval.impl;


import org.opensearch.performanceanalyzer.rca.stats.eval.Statistics;

/** To get the maximum observed value */
public class Max extends MinMaxCommon {

    public Max() {
        super(Long.MIN_VALUE);
    }

    @Override
    boolean shouldUpdate(Number v) {
        return getOldVal().doubleValue() < v.doubleValue();
    }

    @Override
    public Statistics type() {
        return Statistics.MAX;
    }
}
