/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.metric.temperature.capacity;

import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureDimension;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.capacity.calculators.TotalNodeTemperatureCalculator;

public class TotalCpuUtilForTotalNodeMetric extends TotalNodeTemperatureCalculator {

    public TotalCpuUtilForTotalNodeMetric() {
        super(TemperatureDimension.CPU_Utilization);
    }
}
