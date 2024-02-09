/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;

import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Searchbp_Stats extends Metric {
    public Searchbp_Stats(long evaluationIntervalSeconds) {
        super(
                AllMetrics.SearchBackPressureStatsValue.SEARCHBP_TABLE_NAME.toString(),
                evaluationIntervalSeconds);
    }
}
