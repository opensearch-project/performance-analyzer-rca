/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;

import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Heap_AllocRate extends Metric {
    public static final String NAME = AllMetrics.OSMetrics.HEAP_ALLOC_RATE.toString();

    public Heap_AllocRate(long evaluationIntervalSeconds) {
        super(NAME, evaluationIntervalSeconds);
    }
}
