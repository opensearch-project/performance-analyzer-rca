/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.temperature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.temperature.ClusterTemperatureFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.temperature.CompactNodeTemperatureFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.temperature.ClusterTemperatureSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.temperature.CompactClusterLevelNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.temperature.CompactNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureDimension;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureVector;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;

public class ClusterTemperatureRca extends Rca<ClusterTemperatureFlowUnit> {
    private final NodeTemperatureRca nodeTemperatureRca;
    public static final String TABLE_NAME = ClusterTemperatureRca.class.getSimpleName();
    private static final Logger LOG = LogManager.getLogger(ClusterTemperatureRca.class);

    public ClusterTemperatureRca(NodeTemperatureRca nodeTemperatureRca) {
        super(5);
        this.nodeTemperatureRca = nodeTemperatureRca;
    }

    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        throw new IllegalArgumentException(
                name() + "'s generateFlowUnitListFromWire() should not " + "be required.");
    }

    /**
     * @return
     */
    @Override
    public ClusterTemperatureFlowUnit operate() {
        List<CompactNodeTemperatureFlowUnit> flowUnitsAcrossNodes =
                nodeTemperatureRca.getFlowUnits();

        // EachNodeTemperatureRCA should generate a one @{code DimensionalFlowUnit}.
        if (flowUnitsAcrossNodes.size() < 1) {
            LOG.debug("Empty flowUnitsAcrossNodes");
            return new ClusterTemperatureFlowUnit(System.currentTimeMillis());
        }

        AtomicBoolean emptyFlowUnit = new AtomicBoolean(false);
        flowUnitsAcrossNodes.forEach(
                flowUnitForOneNode -> {
                    if (flowUnitForOneNode.isEmpty()) {
                        LOG.debug("Empty flowUnitAcrossOneDimension");
                        emptyFlowUnit.set(true);
                    } else {
                        emptyFlowUnit.set(false);
                    }
                });

        if (emptyFlowUnit.get()) {
            return new ClusterTemperatureFlowUnit(System.currentTimeMillis());
        }

        LOG.error("Number of dataNode Instances in the cluster {}", getDataNodeInstances().size());

        if (flowUnitsAcrossNodes.size() > getDataNodeInstances().size()) {
            LOG.error("Extra flowUnitsAcrossNodes Received {}", flowUnitsAcrossNodes.toString());
            return new ClusterTemperatureFlowUnit(System.currentTimeMillis());
        }

        Map<String, CompactClusterLevelNodeSummary> nodeTemperatureSummaryMap = new HashMap<>();
        final int TOTAL_NODES_IN_CLUSTER = flowUnitsAcrossNodes.size();

        ClusterTemperatureSummary clusterTemperatureSummary =
                new ClusterTemperatureSummary(TOTAL_NODES_IN_CLUSTER);

        // For each dimension go through the temperature profiles sent by each node and figure
        // out the cluster level average along that dimension. Then this method recreates a
        // temperature profile for each of the nodes. This is required because what nodes sent was
        // respect to the nodes but at cluster level they need to be re-calibrated at the cluster
        // level. Note that temperature is a normalized value, normalized by total usage. At node
        // level, the total usage is at the node level (over all shards and shard-independent
        // factors), at the cluster_manager the total usage is the sum over all nodes.
        for (TemperatureDimension dimension : TemperatureDimension.values()) {
            double totalUsageInClusterForDimension = 0.0;
            boolean allFlowUnitSummariesNull = true;
            for (CompactNodeTemperatureFlowUnit nodeFlowUnit : flowUnitsAcrossNodes) {
                CompactNodeSummary summary = nodeFlowUnit.getCompactNodeTemperatureSummary();
                if (summary != null) {
                    totalUsageInClusterForDimension +=
                            summary.getTotalConsumedByDimension(dimension);
                    allFlowUnitSummariesNull = false;
                }
            }
            if (allFlowUnitSummariesNull) {
                continue;
            }

            double nodeAverageForDimension =
                    totalUsageInClusterForDimension / TOTAL_NODES_IN_CLUSTER;
            TemperatureVector.NormalizedValue normalizedAvgForDimension =
                    TemperatureVector.NormalizedValue.calculate(
                            nodeAverageForDimension, totalUsageInClusterForDimension);

            clusterTemperatureSummary.createClusterDimensionalTemperature(
                    dimension, normalizedAvgForDimension, totalUsageInClusterForDimension);

            recalibrateNodeTemperaturesAtClusterLevelUsage(
                    flowUnitsAcrossNodes,
                    nodeTemperatureSummaryMap,
                    dimension,
                    totalUsageInClusterForDimension);
        }
        clusterTemperatureSummary.addNodesSummaries(nodeTemperatureSummaryMap);
        return new ClusterTemperatureFlowUnit(
                System.currentTimeMillis(),
                new ResourceContext(Resources.State.UNKNOWN),
                clusterTemperatureSummary);
    }

    private void recalibrateNodeTemperaturesAtClusterLevelUsage(
            List<CompactNodeTemperatureFlowUnit> flowUnitsAcrossNodes,
            Map<String, CompactClusterLevelNodeSummary> nodeTemperatureSummaryMap,
            TemperatureDimension dimension,
            double totalForDimension) {

        for (CompactNodeTemperatureFlowUnit nodeFlowUnit : flowUnitsAcrossNodes) {
            CompactNodeSummary obtainedNodeTempSummary =
                    nodeFlowUnit.getCompactNodeTemperatureSummary();
            if (obtainedNodeTempSummary == null) {
                continue;
            }
            String key = obtainedNodeTempSummary.getNodeId();

            nodeTemperatureSummaryMap.putIfAbsent(
                    key,
                    new CompactClusterLevelNodeSummary(
                            obtainedNodeTempSummary.getNodeId(),
                            obtainedNodeTempSummary.getHostAddress()));

            CompactClusterLevelNodeSummary constructedCompactNodeTemperatureSummary =
                    nodeTemperatureSummaryMap.get(key);

            double obtainedTotal = obtainedNodeTempSummary.getTotalConsumedByDimension(dimension);

            // This value is the calculated at the cluster level along each dimension.
            // It represents the usage of the resource on this node wrt to the total usage of the
            // resource on other nodes.
            TemperatureVector.NormalizedValue newClusterBasedValue =
                    TemperatureVector.NormalizedValue.calculate(obtainedTotal, totalForDimension);

            constructedCompactNodeTemperatureSummary.setTemperatureForDimension(
                    dimension, newClusterBasedValue);
            constructedCompactNodeTemperatureSummary.setNumOfShards(
                    dimension, obtainedNodeTempSummary.getNumberOfShardsByDimension(dimension));
            constructedCompactNodeTemperatureSummary.setTotalConsumedByDimension(
                    dimension, obtainedTotal);
        }
    }
}
