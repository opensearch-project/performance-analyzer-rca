/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;

import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Disk_Utilization extends Metric {
    public Disk_Utilization(long evaluationIntervalSeconds) {
        super(AllMetrics.DiskValue.DISK_UTILIZATION.name(), evaluationIntervalSeconds);
    }
}
