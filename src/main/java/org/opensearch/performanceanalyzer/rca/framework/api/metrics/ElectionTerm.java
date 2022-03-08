/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class ElectionTerm extends Metric {
    public ElectionTerm(long evaluationIntervalSeconds) {
        super(AllMetrics.ElectionTermValue.ELECTION_TERM.name(), evaluationIntervalSeconds);
    }
}
