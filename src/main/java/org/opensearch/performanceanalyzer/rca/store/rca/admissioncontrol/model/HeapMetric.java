/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.admissioncontrol.model;

/** Represents used heap and max heap in gigabytes */
public class HeapMetric {
    private final double usedHeap;
    private final double maxHeap;

    public HeapMetric(double usedHeap, double maxHeap) {
        this.usedHeap = usedHeap;
        this.maxHeap = maxHeap;
    }

    public double getUsedHeap() {
        return usedHeap;
    }

    public double getMaxHeap() {
        return maxHeap;
    }

    public double getHeapPercent() {
        if (this.getMaxHeap() == 0) {
            return 0;
        }
        return 100 * this.getUsedHeap() / this.getMaxHeap();
    }

    public boolean hasValues() {
        return this.getUsedHeap() != 0 && this.getMaxHeap() != 0;
    }

    @Override
    public String toString() {
        return "HeapMetric{" + "usedHeap=" + usedHeap + ", maxHeap=" + maxHeap + '}';
    }
}
