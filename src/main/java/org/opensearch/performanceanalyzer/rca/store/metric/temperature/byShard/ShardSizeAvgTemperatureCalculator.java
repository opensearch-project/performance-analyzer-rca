/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.metric.temperature.byShard;

import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureDimension;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.byShard.calculators.AvgShardBasedTemperatureCalculator;

/** Class for returning the Average over the sizes of different Shards held by the node. */
public class ShardSizeAvgTemperatureCalculator extends AvgShardBasedTemperatureCalculator {

    public ShardSizeAvgTemperatureCalculator() {
        super(TemperatureDimension.Shard_Size_In_Bytes);
    }
}
