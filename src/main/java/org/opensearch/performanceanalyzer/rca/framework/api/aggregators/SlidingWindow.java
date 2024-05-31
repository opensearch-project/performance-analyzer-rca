/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.aggregators;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/**
 * This is a generic sliding window for RCA sampling. The default behavior is to store the
 * {timestap, value} pair and maintain the sum of all data entries within this sliding window.
 */
public class SlidingWindow<E extends SlidingWindowData> {

    protected final Deque<E> windowDeque;
    protected final long SLIDING_WINDOW_SIZE;
    protected double sum;

    public SlidingWindow(int SLIDING_WINDOW_SIZE_IN_TIMESTAMP, TimeUnit timeUnit) {
        this.windowDeque = new LinkedList<>();
        this.SLIDING_WINDOW_SIZE = timeUnit.toSeconds(SLIDING_WINDOW_SIZE_IN_TIMESTAMP);
        this.sum = 0.0;
    }

    /** callback function when adding a data to the sliding window */
    protected void add(E e) {
        sum += e.getValue();
    }

    /** callback function when removing a data from the sliding window */
    protected void remove(E e) {
        sum -= e.getValue();
    }

    protected void pruneExpiredEntries(long endTimeStamp) {
        while (!windowDeque.isEmpty()
                && TimeUnit.MILLISECONDS.toSeconds(
                                endTimeStamp - windowDeque.peekLast().getTimeStamp())
                        > SLIDING_WINDOW_SIZE) {
            E lastData = windowDeque.pollLast();
            remove(lastData);
        }
    }

    /** insert data into the sliding window */
    public void next(E e) {
        pruneExpiredEntries(e.getTimeStamp());
        add(e);
        windowDeque.addFirst(e);
    }

    /** read the sliding window average based on sliding window size */
    public double readAvg() {
        if (!windowDeque.isEmpty()) {
            return sum / (double) windowDeque.size();
        }
        return Double.NaN;
    }

    /** read the sliding window average based on timestamp */
    public double readAvg(TimeUnit timeUnit) {
        if (windowDeque.isEmpty()) {
            return Double.NaN;
        }
        long timeStampDiff =
                windowDeque.peekFirst().getTimeStamp() - windowDeque.peekLast().getTimeStamp();
        if (timeStampDiff > 0) {
            return sum / ((double) timeStampDiff / (double) timeUnit.toMillis(1));
        }
        return Double.NaN;
    }

    /** read the sliding window sum */
    public double readSum() {
        return this.sum;
    }

    public int size() {
        return windowDeque.size();
    }

    public void clear() {
        this.windowDeque.clear();
    }
}
