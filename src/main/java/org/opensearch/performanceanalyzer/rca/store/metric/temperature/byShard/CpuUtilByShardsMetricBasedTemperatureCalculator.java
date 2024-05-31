/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.metric.temperature.byShard;

import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureDimension;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.byShard.calculators.ShardBasedTemperatureCalculator;

public class CpuUtilByShardsMetricBasedTemperatureCalculator
        extends ShardBasedTemperatureCalculator {

    public CpuUtilByShardsMetricBasedTemperatureCalculator() {
        super(TemperatureDimension.CPU_Utilization);
    }
}
