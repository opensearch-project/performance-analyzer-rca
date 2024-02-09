/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;

import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class HTTP_TotalRequests extends Metric {
    public HTTP_TotalRequests(long evaluationIntervalSeconds) {
        super(AllMetrics.HttpMetric.HTTP_TOTAL_REQUESTS.name(), evaluationIntervalSeconds);
    }
}
