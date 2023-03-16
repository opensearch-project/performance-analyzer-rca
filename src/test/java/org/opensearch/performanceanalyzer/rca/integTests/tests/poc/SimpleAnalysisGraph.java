/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.tests.poc;


import java.util.ArrayList;
import java.util.List;
import org.jooq.Record;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.grpc.HotShardSummaryMessage.CriteriaEnum;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.CPU_Utilization;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotShardSummary;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;
import org.opensearch.performanceanalyzer.rca.store.OpenSearchAnalysisGraph;
import org.opensearch.performanceanalyzer.rca.store.rca.hotshard.IndexShardKey;

public class SimpleAnalysisGraph extends OpenSearchAnalysisGraph {
    public static class NodeRca extends Rca<ResourceFlowUnit<HotNodeSummary>> {
        private final CPU_Utilization cpuUtilization;

        public NodeRca(CPU_Utilization cpu_utilization) {
            super(1);
            this.cpuUtilization = cpu_utilization;
        }

        @Override
        public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
            final List<FlowUnitMessage> flowUnitMessages =
                    args.getWireHopper().readFromWire(args.getNode());
            List<ResourceFlowUnit<HotNodeSummary>> flowUnitList = new ArrayList<>();
            for (FlowUnitMessage flowUnitMessage : flowUnitMessages) {
                flowUnitList.add(ResourceFlowUnit.buildFlowUnitFromWrapper(flowUnitMessage));
            }
            setFlowUnits(flowUnitList);
        }

        @Override
        public ResourceFlowUnit<HotNodeSummary> operate() {
            double maxCpu = 0;
            IndexShardKey indexShardKey = null;
            for (MetricFlowUnit metricFlowUnit : cpuUtilization.getFlowUnits()) {
                if (metricFlowUnit.getData() != null) {
                    // Go through all the entries and find out the shard with the highest CPU
                    // utilization.
                    for (Record record : metricFlowUnit.getData()) {
                        try {
                            String indexName =
                                    record.getValue(
                                            AllMetrics.CommonDimension.INDEX_NAME.toString(),
                                            String.class);
                            // System.out.println(record);
                            Integer shardId =
                                    record.getValue(
                                            AllMetrics.CommonDimension.SHARD_ID.toString(),
                                            Integer.class);
                            if (indexName != null && shardId != null) {
                                double usage = record.getValue(MetricsDB.MAX, Double.class);
                                if (usage > maxCpu) {
                                    maxCpu = usage;
                                    indexShardKey = IndexShardKey.buildIndexShardKey(record);
                                }
                            }
                        } catch (IllegalArgumentException ex) {

                        }
                    }
                }
            }
            InstanceDetails instanceDetails = getInstanceDetails();
            HotNodeSummary nodeSummary =
                    new HotNodeSummary(
                            instanceDetails.getInstanceId(), instanceDetails.getInstanceIp());
            ResourceFlowUnit rfu;
            if (indexShardKey != null) {
                // System.out.println("NodeRca running on " + instanceDetails.getInstanceId());

                HotShardSummary summary =
                        new HotShardSummary(
                                indexShardKey.getIndexName(),
                                String.valueOf(indexShardKey.getShardId()),
                                instanceDetails.getInstanceId().toString(),
                                0);
                summary.setCpuUtilization(maxCpu);
                summary.setCriteria(CriteriaEnum.CPU_UTILIZATION_CRITERIA);
                nodeSummary.appendNestedSummary(summary);
                rfu =
                        new ResourceFlowUnit<>(
                                System.currentTimeMillis(),
                                new ResourceContext(Resources.State.UNHEALTHY),
                                nodeSummary,
                                true);

                // System.out.println("NODE RCA: " + rfu);
            } else {
                rfu = new ResourceFlowUnit<>(System.currentTimeMillis());
            }
            return rfu;
        }
    }

    public static class ClusterRca extends Rca<ResourceFlowUnit<HotClusterSummary>> {
        private final NodeRca nodeRca;

        public ClusterRca(NodeRca nodeRca) {
            super(1);
            this.nodeRca = nodeRca;
        }

        @Override
        public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
            throw new IllegalArgumentException(
                    name() + "'s generateFlowUnitListFromWire() should not " + "be required.");
        }

        // The cluster level RCA goes through all the nodeLevel summaries and then picks the node
        // with the highest CPU and states which shard it is the highest for.
        @Override
        public ResourceFlowUnit<HotClusterSummary> operate() {
            final List<ResourceFlowUnit<HotNodeSummary>> resourceFlowUnits = nodeRca.getFlowUnits();
            HotClusterSummary summary = new HotClusterSummary(getAllClusterInstances().size(), 1);

            final InstanceDetails.Id defaultId = new InstanceDetails.Id("default-id");
            final InstanceDetails.Ip defaultIp = new InstanceDetails.Ip("1.1.1.1");

            InstanceDetails.Id hotNodeId = defaultId;
            InstanceDetails.Ip hotsNodeAddr = defaultIp;
            String hotShard = "";
            String hotShardIndex = "";
            double cpuUtilization = 0.0;

            for (final ResourceFlowUnit<HotNodeSummary> resourceFlowUnit : resourceFlowUnits) {
                if (resourceFlowUnit.isEmpty()) {
                    continue;
                }
                HotNodeSummary nodeSummary = resourceFlowUnit.getSummary();
                HotShardSummary hotShardSummary = nodeSummary.getHotShardSummaryList().get(0);
                double cpu = hotShardSummary.getCpuUtilization();
                if (cpu > cpuUtilization) {
                    hotNodeId = nodeSummary.getNodeID();
                    hotsNodeAddr = nodeSummary.getHostAddress();
                    hotShard = hotShardSummary.getShardId();
                    hotShardIndex = hotShardSummary.getIndexName();
                    cpuUtilization = cpu;
                }
            }

            ResourceFlowUnit<HotClusterSummary> rfu;
            if (!hotNodeId.equals(defaultId)) {
                HotClusterSummary hotClusterSummary =
                        new HotClusterSummary(getAllClusterInstances().size(), 1);
                HotNodeSummary hotNodeSummary = new HotNodeSummary(hotNodeId, hotsNodeAddr);
                HotShardSummary hotShardSummary =
                        new HotShardSummary(hotShardIndex, hotShard, hotNodeId.toString(), 0);
                hotShardSummary.setCpuUtilization(cpuUtilization);
                hotShardSummary.setCriteria(CriteriaEnum.CPU_UTILIZATION_CRITERIA);
                hotNodeSummary.appendNestedSummary(hotShardSummary);
                hotClusterSummary.appendNestedSummary(hotNodeSummary);

                rfu =
                        new ResourceFlowUnit<>(
                                System.currentTimeMillis(),
                                new ResourceContext(Resources.State.UNHEALTHY),
                                hotClusterSummary,
                                true);
            } else {
                rfu = new ResourceFlowUnit<>(System.currentTimeMillis());
            }
            // System.out.println("CLUSTER RCA: " + rfu);
            return rfu;
        }
    }
}
