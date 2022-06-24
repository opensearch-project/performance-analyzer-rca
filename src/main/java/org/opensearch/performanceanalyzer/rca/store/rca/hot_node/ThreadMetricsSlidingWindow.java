/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.hot_node;


import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ThreadMetricsSlidingWindow {

    private final Map<String, Deque<ThreadMetric>> metricsByThreadName;
    private final Map<String, Double> metricSumMap;
    private static final long SLIDING_WINDOW_SIZE_IN_SECONDS = 60;

    public ThreadMetricsSlidingWindow() {
        metricsByThreadName = new HashMap<>();
        metricSumMap = new HashMap<>();
    }

    /** insert data into the sliding window */
    public void next(long timestamp, List<ThreadMetric> threadMetricList) {
        Set<String> newThreadNames = new HashSet<>();
        for (ThreadMetric tm : threadMetricList) {
            Deque<ThreadMetric> windowDeque;
            if (metricsByThreadName.containsKey(tm.getName())) {
                windowDeque = metricsByThreadName.get(tm.getName());
                pruneExpiredEntries(tm.getTimeStamp(), windowDeque);
            } else {
                windowDeque = new LinkedList<>();
                metricsByThreadName.put(tm.getName(), windowDeque);
            }
            windowDeque.addFirst(tm);
            addValue(tm);
            newThreadNames.add(tm.getName());
        }

        for (Map.Entry<String, Deque<ThreadMetric>> entry : metricsByThreadName.entrySet()) {
            if (newThreadNames.contains(entry.getKey())) {
                continue;
            }
            pruneExpiredEntries(timestamp, entry.getValue());
        }
        metricsByThreadName.entrySet().removeIf(e -> e.getValue().size() == 0);
    }

    private void pruneExpiredEntries(long endTimeStamp, Deque<ThreadMetric> windowDeque) {
        while (!windowDeque.isEmpty()
                && TimeUnit.MILLISECONDS.toSeconds(
                                endTimeStamp - windowDeque.peekLast().getTimeStamp())
                        > SLIDING_WINDOW_SIZE_IN_SECONDS) {
            // remove from window
            ThreadMetric prunedData = windowDeque.pollLast();
            // update blocked time sum for thread in new window
            if (prunedData != null) {
                removeValue(prunedData);
            }
        }
    }

    private void removeValue(ThreadMetric prunedData) {
        updateValue(prunedData, false);
    }

    private void addValue(ThreadMetric prunedData) {
        updateValue(prunedData, true);
    }

    private void updateValue(ThreadMetric tm, boolean add) {
        String threadName = tm.getName();
        if (metricSumMap.containsKey(threadName)) {
            double sign = add ? 1d : -1d;
            double newVal = metricSumMap.get(threadName) + sign * tm.getValue();
            if (newVal == 0) {
                metricSumMap.remove(threadName);
            } else {
                metricSumMap.put(threadName, newVal);
            }
        } else if (add) {
            metricSumMap.put(threadName, tm.getValue());
        }
    }

    public int getCountExceedingThreshold(double threshold) {
        return (int) metricSumMap.values().stream().filter(val -> val > threshold).count();
    }

    public double getMaxSum() {
        return metricSumMap.size() > 0 ? Collections.max(metricSumMap.values()) : 0d;
    }
}
