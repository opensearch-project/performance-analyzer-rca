/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Heap_Committed extends Metric {
    public Heap_Committed(long evaluationIntervalSeconds) {
        super(AllMetrics.HeapValue.HEAP_COMMITTED.name(), evaluationIntervalSeconds);
    }
}
