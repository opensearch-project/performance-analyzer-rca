/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class ThreadPool_RejectedReqs extends Metric {
    public static final String NAME =
            AllMetrics.ThreadPoolValue.THREADPOOL_REJECTED_REQS.toString();

    public ThreadPool_RejectedReqs(long evaluationIntervalSeconds) {
        super(NAME, evaluationIntervalSeconds);
    }
}
