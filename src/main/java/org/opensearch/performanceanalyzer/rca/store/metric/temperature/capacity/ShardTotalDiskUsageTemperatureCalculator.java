/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.metric.temperature.capacity;


import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureDimension;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.capacity.calculators.TotalNodeTemperatureCalculator;

/** This class calculated the total disk used by shards in the node. */
public class ShardTotalDiskUsageTemperatureCalculator extends TotalNodeTemperatureCalculator {

    public ShardTotalDiskUsageTemperatureCalculator() {
        super(TemperatureDimension.Shard_Size_In_Bytes);
    }
}
