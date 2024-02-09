/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;

import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class IO_ReadThroughput extends Metric {
    public IO_ReadThroughput(long evaluationIntervalSeconds) {
        super(AllMetrics.OSMetrics.IO_READ_THROUGHPUT.toString(), evaluationIntervalSeconds);
    }
}
