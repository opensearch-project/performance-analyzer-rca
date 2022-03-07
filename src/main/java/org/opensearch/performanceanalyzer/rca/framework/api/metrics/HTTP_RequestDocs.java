/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class HTTP_RequestDocs extends Metric {
    public HTTP_RequestDocs(long evaluationIntervalSeconds) {
        super(AllMetrics.HttpMetric.HTTP_REQUEST_DOCS.name(), evaluationIntervalSeconds);
    }
}
