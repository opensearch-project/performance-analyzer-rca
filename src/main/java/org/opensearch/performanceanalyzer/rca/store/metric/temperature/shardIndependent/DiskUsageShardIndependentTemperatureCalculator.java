/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.metric.temperature.shardIndependent;


import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureDimension;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.shardIndependent.calculators.ShardIndependentTemperatureCalculator;

public class DiskUsageShardIndependentTemperatureCalculator
        extends ShardIndependentTemperatureCalculator {
    public DiskUsageShardIndependentTemperatureCalculator() {
        super(TemperatureDimension.Shard_Size_In_Bytes);
    }
}
