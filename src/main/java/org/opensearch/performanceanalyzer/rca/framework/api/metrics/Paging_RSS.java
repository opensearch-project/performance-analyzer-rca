/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;

import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Paging_RSS extends Metric {
    public Paging_RSS(long evaluationIntervalSeconds) {
        super(AllMetrics.OSMetrics.PAGING_RSS.name(), evaluationIntervalSeconds);
    }
}
