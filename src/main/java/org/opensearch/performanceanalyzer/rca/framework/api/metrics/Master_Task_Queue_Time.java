/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Master_Task_Queue_Time extends Metric {
    public Master_Task_Queue_Time(long evaluationIntervalSeconds) {
        super(
                AllMetrics.MasterMetricValues.MASTER_TASK_QUEUE_TIME.name(),
                evaluationIntervalSeconds);
    }
}
