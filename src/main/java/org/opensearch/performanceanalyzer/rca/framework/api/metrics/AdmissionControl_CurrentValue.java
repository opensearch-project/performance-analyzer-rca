/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;


import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class AdmissionControl_CurrentValue extends Metric {
    public AdmissionControl_CurrentValue(long evaluationIntervalSeconds) {
        super(AllMetrics.AdmissionControlValue.CURRENT_VALUE.toString(), evaluationIntervalSeconds);
    }
}
