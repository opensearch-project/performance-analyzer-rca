/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Sched_Waittime extends Metric {
    public Sched_Waittime(long evaluationIntervalSeconds) {
        super(AllMetrics.OSMetrics.SCHED_WAITTIME.toString(), evaluationIntervalSeconds);
    }
}
