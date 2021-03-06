/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.aggregators;

public class Sum {
    double sum;

    public Sum() {
        sum = 0;
    }

    public void insert(double val) {
        sum += val;
    }

    public double get() {
        return sum;
    }

    public void reset() {
        sum = 0;
    }
}
