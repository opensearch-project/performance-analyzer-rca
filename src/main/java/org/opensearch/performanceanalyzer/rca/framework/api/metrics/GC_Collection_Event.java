/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class GC_Collection_Event extends Metric {
    public static final String NAME = AllMetrics.HeapValue.GC_COLLECTION_EVENT.name();

    public GC_Collection_Event(long evaluationIntervalSeconds) {
        super(AllMetrics.HeapValue.GC_COLLECTION_EVENT.toString(), evaluationIntervalSeconds);
    }
}
