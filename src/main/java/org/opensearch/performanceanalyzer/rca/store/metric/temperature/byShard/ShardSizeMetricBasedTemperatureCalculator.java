/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.metric.temperature.byShard;

import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureDimension;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.byShard.calculators.ShardBasedTemperatureCalculator;

/** Class for returning the Shard Size Metric for individual shards held by the node. */
public class ShardSizeMetricBasedTemperatureCalculator extends ShardBasedTemperatureCalculator {

    public ShardSizeMetricBasedTemperatureCalculator() {
        super(TemperatureDimension.Shard_Size_In_Bytes);
    }
}
