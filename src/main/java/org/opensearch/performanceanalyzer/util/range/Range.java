/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.util.range;


import java.util.Objects;

public class Range {

    private final double lowerBound;
    private final double upperBound;
    private final double threshold;

    public Range(double lowerBound, double upperBound, double threshold) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.threshold = threshold;
    }

    public boolean contains(double value) {
        return value >= lowerBound && value <= upperBound;
    }

    public double getLowerBound() {
        return lowerBound;
    }

    public double getUpperBound() {
        return upperBound;
    }

    public double getThreshold() {
        return threshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Range range = (Range) o;
        return Double.compare(range.getLowerBound(), getLowerBound()) == 0
                && Double.compare(range.getUpperBound(), getUpperBound()) == 0
                && Double.compare(range.getThreshold(), getThreshold()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLowerBound(), getUpperBound(), getThreshold());
    }

    @Override
    public String toString() {
        return String.format(
                "Range{lowerBound=%s, upperBound=%s, threshold=%s}",
                lowerBound, upperBound, threshold);
    }
}
