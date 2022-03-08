/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Heap_Init extends Metric {
    public Heap_Init(long evaluationIntervalSeconds) {
        super(AllMetrics.HeapValue.HEAP_INIT.name(), evaluationIntervalSeconds);
    }
}
