/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class IO_WriteSyscallRate extends Metric {
    public static final String NAME = AllMetrics.OSMetrics.IO_WRITE_SYSCALL_RATE.toString();

    public IO_WriteSyscallRate(long evaluationIntervalSeconds) {
        super(NAME, evaluationIntervalSeconds);
    }
}
