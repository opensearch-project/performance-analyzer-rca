/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class IO_WriteThroughput extends Metric {
    public IO_WriteThroughput(long evaluationIntervalSeconds) {
        super(AllMetrics.OSMetrics.IO_WRITE_THROUGHPUT.toString(), evaluationIntervalSeconds);
    }
}
