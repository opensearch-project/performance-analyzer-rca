/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;

import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Paging_MajfltRate extends Metric {
    public Paging_MajfltRate(long evaluationIntervalSeconds) {
        super(AllMetrics.OSMetrics.PAGING_MAJ_FLT_RATE.toString(), evaluationIntervalSeconds);
    }
}
