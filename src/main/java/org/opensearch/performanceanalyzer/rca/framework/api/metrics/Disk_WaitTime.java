/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Disk_WaitTime extends Metric {
    public Disk_WaitTime(long evaluationIntervalSeconds) {
        super(AllMetrics.DiskValue.DISK_WAITTIME.name(), evaluationIntervalSeconds);
    }
}
