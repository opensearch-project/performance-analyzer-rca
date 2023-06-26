/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Searchbp_Stats extends Metric {
    public static final String NAME = AllMetrics.HeapValue.HEAP_USED.name();

    public Heap_Used(long evaluationIntervalSeconds) {
        super(AllMetrics.HeapValue.HEAP_USED.toString(), evaluationIntervalSeconds);
    }
}
