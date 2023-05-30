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
    // and HEAP_ALLOC_RATE < HEAP_ALLOC_RATE_threshold
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
                                500000,
                                CriteriaEnum.CPU_UTILIZATION_CRITERIA,
                                Resources.State.HEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_2.name(),
                                node.node_1.name(),
                                0.01,
                                1.0E6,
                                CriteriaEnum.HEAP_ALLOC_RATE_CRITERIA,
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
                                1.0E6,
                                CriteriaEnum.CPU_UTILIZATION_CRITERIA,
                                Resources.State.HEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_2.name(),
                                node.node_1.name(),
                                0.01,
                                4.0E5,
                                CriteriaEnum.HEAP_ALLOC_RATE_CRITERIA,
                                Resources.State.HEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_1.name(),
                                node.node_2.name(),
                                0.002,
                                6.0E5,
                                CriteriaEnum.HEAP_ALLOC_RATE_CRITERIA,
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
                                1.1E6,
                                CriteriaEnum.HEAP_ALLOC_RATE_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_1.name(),
                                node.node_2.name(),
                                0.035,
                                1.3E6,
                                CriteriaEnum.CPU_UTILIZATION_CRITERIA,
                                Resources.State.UNHEALTHY)));

        ResourceFlowUnit flowUnit1 = hotShardClusterRca.operate();
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
                                500000,
                                CriteriaEnum.CPU_UTILIZATION_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_2.name(),
                                node.node_1.name(),
                                0.01,
                                550000,
                                CriteriaEnum.CPU_UTILIZATION_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_1.name(),
                                node.node_2.name(),
                                0.011,
                                500000,
                                CriteriaEnum.CPU_UTILIZATION_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_2.name(),
                                shard.shard_1.name(),
                                node.node_1.name(),
                                0.02,
                                500000,
                                CriteriaEnum.CPU_UTILIZATION_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_2.name(),
                                shard.shard_2.name(),
                                node.node_2.name(),
                                0.08,
                                500000,
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

        // 4.3  hot shards across multiple indices as per Heap alloc rate, CPU_Utilization
        // ie. : HEAP_ALLOC_RATE >= HEAP_ALLOC_RATE_threshold
        hotShardRca.mockFlowUnits(
                Arrays.asList(
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_1.name(),
                                node.node_1.name(),
                                0.01,
                                7.0E5,
                                CriteriaEnum.HEAP_ALLOC_RATE_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_2.name(),
                                node.node_1.name(),
                                0.01,
                                6.2E6,
                                CriteriaEnum.HEAP_ALLOC_RATE_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_1.name(),
                                node.node_2.name(),
                                0.01,
                                6.0E5,
                                CriteriaEnum.HEAP_ALLOC_RATE_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_2.name(),
                                node.node_2.name(),
                                0.01,
                                8.9E5,
                                CriteriaEnum.HEAP_ALLOC_RATE_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_2.name(),
                                shard.shard_1.name(),
                                node.node_1.name(),
                                0.01,
                                1.0E7,
                                CriteriaEnum.HEAP_ALLOC_RATE_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_2.name(),
                                shard.shard_2.name(),
                                node.node_2.name(),
                                0.01,
                                8.1E5,
                                CriteriaEnum.HEAP_ALLOC_RATE_CRITERIA,
                                Resources.State.UNHEALTHY)));

        ResourceFlowUnit flowUnit3 = hotShardClusterRca.operate();
        Assert.assertTrue(flowUnit3.getResourceContext().isUnhealthy());

        Assert.assertEquals(1, flowUnit3.getSummary().getNestedSummaryList().size());
        nodeSummary = flowUnit3.getSummary().getNestedSummaryList().get(0);
        Assert.assertEquals(2, nodeSummary.getNestedSummaryList().size());
        List<Object> hotShard3 = nodeSummary.getNestedSummaryList().get(0).getSqlValue();
        List<Object> hotShard4 = nodeSummary.getNestedSummaryList().get(1).getSqlValue();

        //         verify the resource type, Heap alloc rate, node ID, Index Name, shard ID
        Assert.assertEquals(ResourceUtil.HEAP_ALLOC_RATE.getResourceEnumValue(), hotShard3.get(0));
        Assert.assertEquals(ResourceUtil.HEAP_ALLOC_RATE.getResourceEnumValue(), hotShard4.get(0));

        Assert.assertEquals(6.2E6, hotShard3.get(3));
        String[] nodeIndexShardInfo3 = hotShard3.get(8).toString().split(" ");
        Assert.assertEquals(node.node_1.name(), nodeIndexShardInfo3[0]);
        Assert.assertEquals(index.index_1.name(), nodeIndexShardInfo3[1]);
        Assert.assertEquals(shard.shard_2.name(), nodeIndexShardInfo3[2]);

        Assert.assertEquals(1.0E7, hotShard4.get(3));
        String[] nodeIndexShardInfo4 = hotShard4.get(8).toString().split(" ");
        Assert.assertEquals(node.node_1.name(), nodeIndexShardInfo4[0]);
        Assert.assertEquals(index.index_2.name(), nodeIndexShardInfo4[1]);
        Assert.assertEquals(shard.shard_1.name(), nodeIndexShardInfo4[2]);

        // 4.4  hot shards across indices as per Heap Alloc Rate,
        // ie. : HEAP_ALLOC_RATE >= HEAP_ALLOC_RATE_threshold
        // and CPU_UTILIZATION >= CPU_UTILIZATION_threshold
        hotShardRca.mockFlowUnits(
                Arrays.asList(
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_1.name(),
                                node.node_1.name(),
                                0.2,
                                900000,
                                CriteriaEnum.DOUBLE_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_2.name(),
                                node.node_1.name(),
                                0.01,
                                800000,
                                CriteriaEnum.DOUBLE_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_1.name(),
                                node.node_2.name(),
                                0.012,
                                850000,
                                CriteriaEnum.DOUBLE_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_2.name(),
                                shard.shard_1.name(),
                                node.node_1.name(),
                                0.02,
                                8.1E5,
                                CriteriaEnum.DOUBLE_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_2.name(),
                                shard.shard_2.name(),
                                node.node_2.name(),
                                0.025,
                                1.6E6,
                                CriteriaEnum.DOUBLE_CRITERIA,
                                Resources.State.UNHEALTHY)));

        ResourceFlowUnit flowUnit4 = hotShardClusterRca.operate();
        Assert.assertTrue(flowUnit4.getResourceContext().isUnhealthy());

        Assert.assertEquals(1, flowUnit4.getSummary().getNestedSummaryList().size());
        nodeSummary = flowUnit4.getSummary().getNestedSummaryList().get(0);
        Assert.assertEquals(2, nodeSummary.getNestedSummaryList().size());
        List<Object> hotShard5 = nodeSummary.getNestedSummaryList().get(0).getSqlValue();
        List<Object> hotShard6 = nodeSummary.getNestedSummaryList().get(1).getSqlValue();

        // verify the resource type, cpu usage and heap alloc rate, node ID, Index Name, shard ID
        Assert.assertEquals(ResourceUtil.CPU_USAGE.getResourceEnumValue(), hotShard5.get(0));
        Assert.assertEquals(ResourceUtil.HEAP_ALLOC_RATE.getResourceEnumValue(), hotShard6.get(0));

        Assert.assertEquals(0.2, hotShard5.get(3));
        String[] nodeIndexShardInfo5 = hotShard5.get(8).toString().split(" ");
        Assert.assertEquals(node.node_1.name(), nodeIndexShardInfo5[0]);
        Assert.assertEquals(index.index_1.name(), nodeIndexShardInfo5[1]);
        Assert.assertEquals(shard.shard_1.name(), nodeIndexShardInfo5[2]);

        Assert.assertEquals(1.6E6, hotShard6.get(3));
        String[] nodeIndexShardInfo6 = hotShard6.get(8).toString().split(" ");
        Assert.assertEquals(node.node_2.name(), nodeIndexShardInfo6[0]);
        Assert.assertEquals(index.index_2.name(), nodeIndexShardInfo6[1]);
        Assert.assertEquals(shard.shard_2.name(), nodeIndexShardInfo6[2]);
    }

    // 5. UnHealthy FlowUnits received, hot shard identification on multiple Dimension
    @Test
    public void testOperateForHotShardonMultipleDimension() {
        // CPU_UTILIZATION >= CPU_UTILIZATION_threshold, HEAP_ALLOC_RATE >=
        // HEAP_ALLOC_RATE_threshold
        hotShardRca.mockFlowUnits(
                Arrays.asList(
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_1.name(),
                                node.node_1.name(),
                                0.3,
                                420000,
                                CriteriaEnum.CPU_UTILIZATION_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_2.name(),
                                node.node_1.name(),
                                0.28,
                                9.0E6,
                                CriteriaEnum.HEAP_ALLOC_RATE_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_1.name(),
                                node.node_2.name(),
                                0.01,
                                380000,
                                CriteriaEnum.CPU_UTILIZATION_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_1.name(),
                                shard.shard_1.name(),
                                node.node_2.name(),
                                0.25,
                                1.2E6,
                                CriteriaEnum.HEAP_ALLOC_RATE_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_2.name(),
                                shard.shard_1.name(),
                                node.node_1.name(),
                                0.28,
                                1.5E6,
                                CriteriaEnum.HEAP_ALLOC_RATE_CRITERIA,
                                Resources.State.UNHEALTHY),
                        RcaTestHelper.generateFlowUnitForHotShard(
                                index.index_2.name(),
                                shard.shard_2.name(),
                                node.node_2.name(),
                                0.32,
                                1.6E6,
                                CriteriaEnum.HEAP_ALLOC_RATE_CRITERIA,
                                Resources.State.UNHEALTHY)));

        ResourceFlowUnit flowUnit2 = hotShardClusterRca.operate();
        Assert.assertTrue(flowUnit2.getResourceContext().isUnhealthy());

        Assert.assertEquals(1, flowUnit2.getSummary().getNestedSummaryList().size());
        GenericSummary nodeSummary = flowUnit2.getSummary().getNestedSummaryList().get(0);
        Assert.assertEquals(2, nodeSummary.getNestedSummaryList().size());
        List<Object> hotShard1 = nodeSummary.getNestedSummaryList().get(0).getSqlValue();
        List<Object> hotShard2 = nodeSummary.getNestedSummaryList().get(1).getSqlValue();

        Assert.assertEquals(ResourceUtil.CPU_USAGE.getResourceEnumValue(), hotShard1.get(0));
        Assert.assertEquals(ResourceUtil.HEAP_ALLOC_RATE.getResourceEnumValue(), hotShard2.get(0));

        // verify the resource type, cpu utilization value, node ID, Index Name, shard ID
        Assert.assertEquals(0.3, hotShard1.get(3));
        String[] nodeIndexShardInfo1 = hotShard1.get(8).toString().split(" ");
        Assert.assertEquals(node.node_1.name(), nodeIndexShardInfo1[0]);
        Assert.assertEquals(index.index_1.name(), nodeIndexShardInfo1[1]);
        Assert.assertEquals(shard.shard_1.name(), nodeIndexShardInfo1[2]);

        // verify the resource type, heap alloc rate, node ID, Index Name, shard ID
        Assert.assertEquals(9.0E6, hotShard2.get(3));
        String[] nodeIndexShardInfo2 = hotShard2.get(8).toString().split(" ");
        Assert.assertEquals(node.node_1.name(), nodeIndexShardInfo2[0]);
        Assert.assertEquals(index.index_1.name(), nodeIndexShardInfo2[1]);
        Assert.assertEquals(shard.shard_2.name(), nodeIndexShardInfo2[2]);
    }
}
