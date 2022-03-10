/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class GC_Type extends Metric {

    public static final String NAME = AllMetrics.GCInfoValue.GARBAGE_COLLECTOR_TYPE.toString();

    public GC_Type(long evaluationIntervalSeconds) {
        super(NAME, evaluationIntervalSeconds);
    }
}
