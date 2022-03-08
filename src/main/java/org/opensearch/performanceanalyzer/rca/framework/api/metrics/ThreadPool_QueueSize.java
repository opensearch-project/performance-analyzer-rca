/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class ThreadPool_QueueSize extends Metric {
    public ThreadPool_QueueSize(long evaluationIntervalSeconds) {
        super(
                AllMetrics.ThreadPoolValue.THREADPOOL_QUEUE_SIZE.toString(),
                evaluationIntervalSeconds);
    }
}
