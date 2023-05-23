/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class ThreadPool_TotalThreads extends Metric {
    public ThreadPool_TotalThreads(long evaluationIntervalSeconds) {
        super(
                AllMetrics.ThreadPoolValue.THREADPOOL_TOTAL_THREADS.toString(),
                evaluationIntervalSeconds);
    }
}
