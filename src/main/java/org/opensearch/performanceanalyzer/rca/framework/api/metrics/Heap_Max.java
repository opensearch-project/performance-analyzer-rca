/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;

import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Heap_Max extends Metric {
    public static final String NAME = AllMetrics.HeapValue.HEAP_MAX.name();

    public Heap_Max(long evaluationIntervalSeconds) {
        super(AllMetrics.HeapValue.HEAP_MAX.name(), evaluationIntervalSeconds);
    }
}
