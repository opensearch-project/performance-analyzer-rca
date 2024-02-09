/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.metric.temperature.byShard;

import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureDimension;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.byShard.calculators.AvgShardBasedTemperatureCalculator;

public class HeapAllocRateByShardAvgTemperatureCalculator
        extends AvgShardBasedTemperatureCalculator {

    public HeapAllocRateByShardAvgTemperatureCalculator() {
        super(TemperatureDimension.Heap_AllocRate);
    }
}
