/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.searchbackpressure.model;

/** Represents used heap and max heap in gigabytes */
public class SearchBackPressureRCAMetric {
    private final double usedHeap;
    private final double maxHeap;
    private final double searchbpShardCancellationCount;
    private final double searchbpTaskCancellationCount;
    private final double searchbpJVMShardCancellationCount;
    private final double searchbpJVMTaskCancellationCount;

    // Constructor
    public SearchBackPressureRCAMetric(double usedHeap, double maxHeap, double searchbpShardCancellationCount,
            double searchbpTaskCancellationCount, double searchbpJVMShardCancellationCount,
            double searchbpJVMTaskCancellationCount) {
        this.usedHeap = usedHeap;
        this.maxHeap = maxHeap;
        this.searchbpShardCancellationCount = searchbpShardCancellationCount;
        this.searchbpTaskCancellationCount = searchbpTaskCancellationCount;
        this.searchbpJVMShardCancellationCount = searchbpJVMShardCancellationCount;
        this.searchbpJVMTaskCancellationCount = searchbpJVMTaskCancellationCount;
    }

    // Getters
    public double getUsedHeap() {
        return usedHeap;
    }

    public double getMaxHeap() {
        return maxHeap;
    }

    public double getSearchbpShardCancellationCount() {
        return searchbpShardCancellationCount;
    }

    public double getSearchbpTaskCancellationCount() {
        return searchbpTaskCancellationCount;
    }

    public double getSearchbpJVMShardCancellationCount() {
        return searchbpJVMShardCancellationCount;
    }

    public double getSearchbpJVMTaskCancellationCount() {
        return searchbpJVMTaskCancellationCount;
    }

    public double getHeapUsagePercent() {
        if (this.getMaxHeap() == 0) {
            return 0;
        }
        return 100 * this.getUsedHeap() / this.getMaxHeap();
    }

    public double getShardJVMCancellationPercent() {
        if (this.getSearchbpShardCancellationCount() == 0) {
            return 0;
        }
        return 100 * this.getSearchbpJVMShardCancellationCount() / this.getSearchbpShardCancellationCount();
    }

    public double getTaskJVMCancellationPercent() {
        if (this.getSearchbpTaskCancellationCount() == 0) {
            return 0;
        }
        return 100 * this.getSearchbpJVMTaskCancellationCount() / this.getSearchbpTaskCancellationCount();
    }

    public boolean hasValues() {
        return this.getUsedHeap() != 0 && this.getMaxHeap() != 0;
    }

    @Override
    public String toString() {
        return "HeapMetric{" + "usedHeap=" + usedHeap + ", maxHeap=" + maxHeap + '}';
    }
}
