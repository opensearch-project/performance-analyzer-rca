/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;

import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class IO_TotThroughput extends Metric {
    public static final String NAME = AllMetrics.OSMetrics.IO_TOT_THROUGHPUT.toString();

    public IO_TotThroughput(long evaluationIntervalSeconds) {
        super(NAME, evaluationIntervalSeconds);
    }
}
