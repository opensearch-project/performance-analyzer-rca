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
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.byShard.AvgCpuUtilByShardsMetricBasedTemperatureCalculator;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.byShard.CpuUtilByShardsMetricBasedTemperatureCalculator;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.capacity.TotalCpuUtilForTotalNodeMetric;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.shardIndependent.ShardIndependentTemperatureCalculatorCpuUtilMetric;
import org.opensearch.performanceanalyzer.rca.store.rca.temperature.DimensionalTemperatureCalculator;

public class CpuUtilDimensionTemperatureRca extends Rca<DimensionalTemperatureFlowUnit> {
    private static final Logger LOG = LogManager.getLogger(CpuUtilDimensionTemperatureRca.class);

    private final TotalCpuUtilForTotalNodeMetric CPU_UTIL_PEAK_USAGE;
    private final CpuUtilByShardsMetricBasedTemperatureCalculator CPU_UTIL_BY_SHARD;
    private final AvgCpuUtilByShardsMetricBasedTemperatureCalculator AVG_CPU_UTIL_BY_SHARD;
    private final ShardIndependentTemperatureCalculatorCpuUtilMetric CPU_UTIL_SHARD_INDEPENDENT;

    private final ShardStore shardStore;

    public static final TemperatureVector.NormalizedValue
            THRESHOLD_NORMALIZED_VAL_FOR_HEAT_ZONE_ASSIGNMENT =
                    new TemperatureVector.NormalizedValue((short) 2);

    public CpuUtilDimensionTemperatureRca(
            final long evaluationIntervalSeconds,
            ShardStore shardStore,
            CpuUtilByShardsMetricBasedTemperatureCalculator cpuUtilByShard,
            AvgCpuUtilByShardsMetricBasedTemperatureCalculator avgCpuUtilByShards,
            ShardIndependentTemperatureCalculatorCpuUtilMetric shardIndependentCpuUtilMetric,
            TotalCpuUtilForTotalNodeMetric cpuUtilPeakUsage) {
        super(evaluationIntervalSeconds);
        this.CPU_UTIL_PEAK_USAGE = cpuUtilPeakUsage;
        this.CPU_UTIL_BY_SHARD = cpuUtilByShard;
        this.CPU_UTIL_SHARD_INDEPENDENT = shardIndependentCpuUtilMetric;
        this.AVG_CPU_UTIL_BY_SHARD = avgCpuUtilByShards;
        this.shardStore = shardStore;
    }

    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        throw new IllegalArgumentException("Generating from wire is not required.");
    }

    @Override
    public DimensionalTemperatureFlowUnit operate() {
        DimensionalTemperatureFlowUnit cpuUtilTemperatureFlowUnit =
                DimensionalTemperatureCalculator.getTemperatureForDimension(
                        shardStore,
                        TemperatureDimension.CPU_Utilization,
                        CPU_UTIL_BY_SHARD,
                        AVG_CPU_UTIL_BY_SHARD,
                        CPU_UTIL_SHARD_INDEPENDENT,
                        CPU_UTIL_PEAK_USAGE,
                        THRESHOLD_NORMALIZED_VAL_FOR_HEAT_ZONE_ASSIGNMENT);
        LOG.info(
                "CPU Utilization temperature calculated: {}",
                cpuUtilTemperatureFlowUnit.getNodeDimensionProfile());
        return cpuUtilTemperatureFlowUnit;
    }
}
