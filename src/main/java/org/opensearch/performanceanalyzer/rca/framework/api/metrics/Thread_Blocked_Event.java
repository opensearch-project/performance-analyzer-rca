/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Thread_Blocked_Event extends Metric {
    public Thread_Blocked_Event(long evaluationIntervalSeconds) {
        super(AllMetrics.OSMetrics.THREAD_BLOCKED_EVENT.name(), evaluationIntervalSeconds);
    }
}
