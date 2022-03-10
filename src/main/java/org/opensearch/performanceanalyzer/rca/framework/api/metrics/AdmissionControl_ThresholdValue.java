/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class AdmissionControl_ThresholdValue extends Metric {
    public AdmissionControl_ThresholdValue(long evaluationIntervalSeconds) {
        super(
                AllMetrics.AdmissionControlValue.THRESHOLD_VALUE.toString(),
                evaluationIntervalSeconds);
    }
}
