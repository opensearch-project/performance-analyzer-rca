/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class SearchBackPressureStats extends Metric {
    public SearchBackPressureStats(long evaluationIntervalSeconds) {
        super("searchbp_metric", evaluationIntervalSeconds);
    }
}
