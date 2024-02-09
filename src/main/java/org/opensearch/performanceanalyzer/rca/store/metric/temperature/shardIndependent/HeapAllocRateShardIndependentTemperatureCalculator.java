/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.metric.temperature.shardIndependent;

import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureDimension;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.shardIndependent.calculators.ShardIndependentTemperatureCalculator;

public class HeapAllocRateShardIndependentTemperatureCalculator
        extends ShardIndependentTemperatureCalculator {

    public HeapAllocRateShardIndependentTemperatureCalculator() {
        super(TemperatureDimension.Heap_AllocRate);
    }
}
