/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.temperature.dimension;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.temperature.DimensionalTemperatureFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.ShardStore;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureDimension;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureVector;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.byShard.ShardSizeAvgTemperatureCalculator;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.byShard.ShardSizeMetricBasedTemperatureCalculator;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.capacity.ShardTotalDiskUsageTemperatureCalculator;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.shardIndependent.DiskUsageShardIndependentTemperatureCalculator;
import org.opensearch.performanceanalyzer.rca.store.rca.temperature.DimensionalTemperatureCalculator;

/*
 *Returns the shard size based heat of an individual node.
 */

public class ShardSizeDimensionTemperatureRca extends Rca<DimensionalTemperatureFlowUnit> {

    private static final Logger LOG = LogManager.getLogger(ShardSizeDimensionTemperatureRca.class);
    // The threshold set here is an initial threshold only.
    // TODO: Update the threshold appropriately after testing so that we assign heat correctly.
    private static final TemperatureVector.NormalizedValue
            THRESHOLD_NORMALIZED_VAL_FOR_HEAT_ZONE_ASSIGNMENT =
                    new TemperatureVector.NormalizedValue((short) 2);
    private final ShardSizeMetricBasedTemperatureCalculator SHARD_SIZE_BY_SHARD;
    private final ShardSizeAvgTemperatureCalculator SHARD_SIZE_AVG;
    private final ShardTotalDiskUsageTemperatureCalculator SHARD_TOTAL_USAGE;
    private final ShardStore SHARD_STORE;

    public ShardSizeDimensionTemperatureRca(
            final long evaluationIntervalSeconds,
            final ShardStore shardStore,
            final ShardSizeMetricBasedTemperatureCalculator shardSizeByShard,
            final ShardSizeAvgTemperatureCalculator shardSizeAvg,
            final ShardTotalDiskUsageTemperatureCalculator shardTotalDiskUsage) {
        super(evaluationIntervalSeconds);
        this.SHARD_STORE = shardStore;
        this.SHARD_SIZE_BY_SHARD = shardSizeByShard;
        this.SHARD_SIZE_AVG = shardSizeAvg;
        this.SHARD_TOTAL_USAGE = shardTotalDiskUsage;
    }

    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        throw new IllegalStateException(
                "This node: ["
                        + name()
                        + "] should not have received flow "
                        + "units from remote nodes.");
    }

    @Override
    public DimensionalTemperatureFlowUnit operate() {
        LOG.debug("executing : {}", name());
        DimensionalTemperatureFlowUnit shardSizeTemperatureFlowUnit =
                DimensionalTemperatureCalculator.getTemperatureForDimension(
                        SHARD_STORE,
                        TemperatureDimension.Shard_Size_In_Bytes,
                        SHARD_SIZE_BY_SHARD,
                        SHARD_SIZE_AVG,
                        new DiskUsageShardIndependentTemperatureCalculator(),
                        SHARD_TOTAL_USAGE,
                        THRESHOLD_NORMALIZED_VAL_FOR_HEAT_ZONE_ASSIGNMENT);
        LOG.info(
                "Shard Size temperature calculated: {}",
                shardSizeTemperatureFlowUnit.getNodeDimensionProfile());
        return shardSizeTemperatureFlowUnit;
    }
}
