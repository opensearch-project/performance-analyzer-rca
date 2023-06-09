/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class SearchBackPressureStats_Test extends Metric {
    public SearchBackPressureStats_Test(long evaluationIntervalSeconds) {
        super("SearchBackPressureStats_Test", evaluationIntervalSeconds);
    }
}
