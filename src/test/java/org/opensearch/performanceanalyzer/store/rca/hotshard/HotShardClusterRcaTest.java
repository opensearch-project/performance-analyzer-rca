/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.store.rca.hotshard;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.grpc.HotShardSummaryMessage.CriteriaEnum;
import org.opensearch.performanceanalyzer.rca.GradleTaskForRca;
import org.opensearch.performanceanalyzer.rca.framework.api.RcaTestHelper;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericSummary;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.store.rca.hotshard.HotShardClusterRca;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;

@Category(GradleTaskForRca.class)
public class HotShardClusterRcaTest {

    private RcaTestHelper hotShardRca;

    private HotShardClusterRca hotShardClusterRca;

    private enum index {
        index_1,
        index_2
    }

    private enum shard {
        shard_1,
        shard_2,
        shard_3
    }

    private enum node {
        node_1,
        node_2,
    }

    @Before
    public void setup() {
        // setup cluster details
        try {
            hotShardRca = new RcaTestHelper<HotNodeSummary>();
            hotShardClusterRca = new HotShardClusterRca(1, hotShardRca);

            InstanceDetails instanceDetails =
                    new InstanceDetails(
                            AllMetrics.NodeRole.DATA,
                            new InstanceDetails.Id("node1"),
                            new InstanceDetails.Ip("127.0.0.1"),
                            false);
            ClusterDetailsEventProcessor clusterDetailsEventProcessor =
                    new ClusterDetailsEventProcessor();
            clusterDetailsEventProcessor.setNodesDetails(
                    Collections.singletonList(
                            new ClusterDetailsEventProcessor.NodeDetails(
                                    AllMetrics.NodeRole.DATA, "node1", "127.0.0.1", false)));
            AppContext appContext = new AppContext();
            appContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);

            hotShardRca.setAppContext(appContext);
            hotShardClusterRca.setAppContext(appContext);
        } catch (Exception e) {
            Assert.assertTrue("Exception when generating cluster details event", false);
            return;
        }
    }

    // 1. No Flow Units received/generated on cluster_manager
    @Test
    public void testOperateForMissingFlowUnits() {
        ResourceFlowUnit flowUnit = hotShardClusterRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());
    }

    // 2. Empty Flow Units received/generated on cluster_manager
    @Test
    public void testOperateForEmptyFlowUnits() {
        hotShardRca.mockFlowUnits(Collections.emptyList());

        ResourceFlowUnit flowUnit = hotShardClusterRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());
    }

    // 3. Healthy FlowUnits received, i.e :
    // CPU_UTILIZATION < CPU_UTILIZATION_threshold
    @Test
    public void testOperateForHealthyFlowUnits() {
        // 3.1  Flow Units received from single node
        hotShardRca.mockFlowUnits(
                Arrays.asList(
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_1.name(),
                                node.node_1.name(),
                                0.04,
                                CriteriaEnum.CPU_UTILIZATION_CRITERIA,
                                Resources.State.HEALTHY)));

        ResourceFlowUnit flowUnit = hotShardClusterRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());
        Assert.assertTrue(flowUnit.getSummary().getNestedSummaryList().isEmpty());

        // 3.2 FlowUnits received from both nodes
        hotShardRca.mockFlowUnits(
                Arrays.asList(
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_1.name(),
                                node.node_1.name(),
                                0.02,
                                CriteriaEnum.CPU_UTILIZATION_CRITERIA,
                                Resources.State.HEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_2.name(),
                                node.node_1.name(),
                                0.01,
                                CriteriaEnum.CPU_UTILIZATION_CRITERIA,
                                Resources.State.HEALTHY)));
    }

    // 4. UnHealthy FlowUnits received, hot shard identification on single Dimension
    @Test
    public void testOperateForHotShardonSingleDimension() {
        // 4.1  No hot shard across an index
        hotShardRca.mockFlowUnits(
                Arrays.asList(
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_1.name(),
                                node.node_1.name(),
                                0.03,
                                CriteriaEnum.CPU_UTILIZATION_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_1.name(),
                                node.node_2.name(),
                                0.035,
                                CriteriaEnum.CPU_UTILIZATION_CRITERIA,
                                Resources.State.UNHEALTHY)));

        ResourceFlowUnit<HotClusterSummary> flowUnit1 = hotShardClusterRca.operate();
        Assert.assertFalse(flowUnit1.getResourceContext().isUnhealthy());
        Assert.assertTrue(flowUnit1.getSummary().getNestedSummaryList().isEmpty());

        // 4.2  hot shards across an index as per CPU Utilization, ie. : CPU_UTILIZATION >=
        // CPU_UTILIZATION_threshold
        hotShardRca.mockFlowUnits(
                Arrays.asList(
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_1.name(),
                                node.node_1.name(),
                                0.075,
                                CriteriaEnum.CPU_UTILIZATION_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_2.name(),
                                node.node_1.name(),
                                0.01,
                                CriteriaEnum.CPU_UTILIZATION_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_1.name(),
                                node.node_2.name(),
                                0.011,
                                CriteriaEnum.CPU_UTILIZATION_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_2.name(),
                                shard.shard_1.name(),
                                node.node_1.name(),
                                0.02,
                                CriteriaEnum.CPU_UTILIZATION_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_2.name(),
                                shard.shard_2.name(),
                                node.node_2.name(),
                                0.08,
                                CriteriaEnum.CPU_UTILIZATION_CRITERIA,
                                Resources.State.UNHEALTHY)));

        ResourceFlowUnit flowUnit2 = hotShardClusterRca.operate();
        Assert.assertTrue(flowUnit2.getResourceContext().isUnhealthy());

        Assert.assertEquals(1, flowUnit2.getSummary().getNestedSummaryList().size());
        GenericSummary nodeSummary = flowUnit2.getSummary().getNestedSummaryList().get(0);
        Assert.assertEquals(2, nodeSummary.getNestedSummaryList().size());
        List<Object> hotShard1 = nodeSummary.getNestedSummaryList().get(0).getSqlValue();
        List<Object> hotShard2 = nodeSummary.getNestedSummaryList().get(1).getSqlValue();

        // verify the resource type, cpu utilization value, node ID, Index Name, shard ID
        Assert.assertEquals(ResourceUtil.CPU_USAGE.getResourceEnumValue(), hotShard1.get(0));
        Assert.assertEquals(ResourceUtil.CPU_USAGE.getResourceEnumValue(), hotShard2.get(0));

        Assert.assertEquals(0.075, hotShard1.get(3));
        String[] nodeIndexShardInfo1 = hotShard1.get(8).toString().split(" ");
        Assert.assertEquals(node.node_1.name(), nodeIndexShardInfo1[0]);
        Assert.assertEquals(index.index_1.name(), nodeIndexShardInfo1[1]);
        Assert.assertEquals(shard.shard_1.name(), nodeIndexShardInfo1[2]);

        Assert.assertEquals(0.08, hotShard2.get(3));
        String[] nodeIndexShardInfo2 = hotShard2.get(8).toString().split(" ");
        Assert.assertEquals(node.node_2.name(), nodeIndexShardInfo2[0]);
        Assert.assertEquals(index.index_2.name(), nodeIndexShardInfo2[1]);
        Assert.assertEquals(shard.shard_2.name(), nodeIndexShardInfo2[2]);
    }
}
