/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class AdmissionControl_RejectionCount extends Metric {
    public AdmissionControl_RejectionCount(long evaluationIntervalSeconds) {
        super(
                AllMetrics.AdmissionControlValue.REJECTION_COUNT.toString(),
                evaluationIntervalSeconds);
    }
}
