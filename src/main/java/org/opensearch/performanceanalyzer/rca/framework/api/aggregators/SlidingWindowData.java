/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.aggregators;

/**
 * SlidingWindowData holds a timestamp in ms and a double value. It is the basic datatype used by a
 * {@link SlidingWindow}
 */
public class SlidingWindowData {
    protected long timeStamp;
    protected double value;

    public SlidingWindowData() {
        this.timeStamp = -1;
        this.value = -1;
    }

    public SlidingWindowData(long timeStamp, double value) {
        this.timeStamp = timeStamp;
        this.value = value;
    }

    public long getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public double getValue() {
        return this.value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
