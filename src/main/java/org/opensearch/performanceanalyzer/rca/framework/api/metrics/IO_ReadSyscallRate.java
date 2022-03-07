/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class IO_ReadSyscallRate extends Metric {
    public static final String NAME = AllMetrics.OSMetrics.IO_READ_SYSCALL_RATE.name();

    public IO_ReadSyscallRate(long evaluationIntervalSeconds) {
        super(NAME, evaluationIntervalSeconds);
    }
}
