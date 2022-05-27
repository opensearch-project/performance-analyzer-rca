/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.hot_node;

public class ThreadMetric {
    private final String name;
    private final double value;
    private final long timeStamp;

    private final String operation;

    public ThreadMetric(String threadName, double val, long timeStamp, String operation) {
        this.name = threadName;
        this.value = val;
        this.timeStamp = timeStamp;
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public String getName() {
        return name;
    }

    public double getValue() {
        return value;
    }
}
