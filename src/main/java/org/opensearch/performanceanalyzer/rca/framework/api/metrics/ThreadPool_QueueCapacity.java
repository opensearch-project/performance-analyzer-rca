/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;

import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class ThreadPool_QueueCapacity extends Metric {
    public static final String NAME =
            AllMetrics.ThreadPoolValue.THREADPOOL_QUEUE_CAPACITY.toString();

    public ThreadPool_QueueCapacity() {
        super(NAME, 5);
    }
}
