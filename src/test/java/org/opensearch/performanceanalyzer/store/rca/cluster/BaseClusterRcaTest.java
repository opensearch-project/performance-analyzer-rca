/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.store.rca.cluster;

import static java.time.Instant.ofEpochMilli;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.GradleTaskForRca;
import org.opensearch.performanceanalyzer.rca.framework.api.RcaTestHelper;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.BaseClusterRca;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessorTestHelper;

@Category(GradleTaskForRca.class)
public class BaseClusterRcaTest {
    private BaseClusterRca clusterRca;
    private RcaTestHelper<HotNodeSummary> nodeRca;
    private RcaTestHelper<HotNodeSummary> nodeRca2;
    private Resource type1;
    private Resource type2;
    private Resource invalidType;
    private AppContext appContext;

    @Before
    public void init() {
        ClusterDetailsEventProcessor clusterDetailsEventProcessor =
                new ClusterDetailsEventProcessor();
        ClusterDetailsEventProcessor.NodeDetails node1 =
                new ClusterDetailsEventProcessor.NodeDetails(
                        AllMetrics.NodeRole.DATA, "node1", "127.0.0.0", false);
        ClusterDetailsEventProcessor.NodeDetails node2 =
                new ClusterDetailsEventProcessor.NodeDetails(
                        AllMetrics.NodeRole.DATA, "node2", "127.0.0.1", false);
        ClusterDetailsEventProcessor.NodeDetails node3 =
                new ClusterDetailsEventProcessor.NodeDetails(
                        AllMetrics.NodeRole.DATA, "node3", "127.0.0.2", false);
        ClusterDetailsEventProcessor.NodeDetails cluster_manager =
                new ClusterDetailsEventProcessor.NodeDetails(
                        AllMetrics.NodeRole.ELECTED_CLUSTER_MANAGER,
                        "cluster_manager",
                        "127.0.0.9",
                        true);

        List<ClusterDetailsEventProcessor.NodeDetails> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        nodes.add(node3);
        nodes.add(cluster_manager);
        clusterDetailsEventProcessor.setNodesDetails(nodes);

        appContext = new AppContext();
        appContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);

        nodeRca = new RcaTestHelper<>("RCA1");
        nodeRca.setAppContext(appContext);

        nodeRca2 = new RcaTestHelper<>("RCA2");
        nodeRca2.setAppContext(appContext);

        invalidType = ResourceUtil.OLD_GEN_HEAP_USAGE;

        clusterRca = new BaseClusterRca(1, nodeRca, nodeRca2);
        clusterRca.setAppContext(appContext);

        type1 = ResourceUtil.OLD_GEN_HEAP_USAGE;
        type2 = ResourceUtil.CPU_USAGE;
    }

    @Test
    public void testUnhealthyFlowunit() throws ClassCastException {
        ResourceFlowUnit<HotClusterSummary> flowUnit;
        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        type1, "node1", "127.0.0.0", Resources.State.UNHEALTHY),
                RcaTestHelper.generateFlowUnit(
                        type2, "node2", "127.0.0.1", Resources.State.HEALTHY));

        flowUnit = clusterRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        HotClusterSummary clusterSummary = flowUnit.getSummary();
        Assert.assertEquals(1, clusterSummary.getNumOfUnhealthyNodes());
        Assert.assertTrue(
                compareNodeSummary("node1", type1, clusterSummary.getHotNodeSummaryList().get(0)));

        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        type1, "node1", "127.0.0.0", Resources.State.HEALTHY),
                RcaTestHelper.generateFlowUnit(
                        type2, "node2", "127.0.0.1", Resources.State.HEALTHY));

        flowUnit = clusterRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        type1, "node1", "127.0.0.0", Resources.State.UNHEALTHY),
                RcaTestHelper.generateFlowUnit(
                        type2, "node2", "127.0.0.1", Resources.State.UNHEALTHY));

        flowUnit = clusterRca.operate();

        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        clusterSummary = flowUnit.getSummary();
        Assert.assertEquals(2, clusterSummary.getNumOfUnhealthyNodes());
        if (compareNodeSummary("node1", type1, clusterSummary.getHotNodeSummaryList().get(0))) {
            Assert.assertTrue(
                    compareNodeSummary(
                            "node2", type2, clusterSummary.getHotNodeSummaryList().get(1)));
        } else {
            Assert.assertTrue(
                    compareNodeSummary(
                            "node1", type1, clusterSummary.getHotNodeSummaryList().get(1)));
            Assert.assertTrue(
                    compareNodeSummary(
                            "node1", type1, clusterSummary.getHotNodeSummaryList().get(0)));
        }
    }

    @Test
    public void testMultipleRcas() throws ClassCastException {
        ResourceFlowUnit<HotClusterSummary> flowUnit;
        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        type1, "node1", "127.0.0.0", Resources.State.UNHEALTHY),
                RcaTestHelper.generateFlowUnit(
                        type1, "node2", "127.0.0.1", Resources.State.HEALTHY));

        nodeRca2.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        type2, "node1", "127.0.0.0", Resources.State.HEALTHY),
                RcaTestHelper.generateFlowUnit(
                        type2, "node2", "127.0.0.1", Resources.State.HEALTHY));

        flowUnit = clusterRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        HotClusterSummary clusterSummary = flowUnit.getSummary();
        Assert.assertEquals(1, clusterSummary.getNumOfUnhealthyNodes());
        Assert.assertTrue(
                compareNodeSummary("node1", type1, clusterSummary.getHotNodeSummaryList().get(0)));

        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        type1, "node1", "127.0.0.0", Resources.State.UNHEALTHY),
                RcaTestHelper.generateFlowUnit(
                        type1, "node2", "127.0.0.1", Resources.State.HEALTHY));

        nodeRca2.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        type2, "node1", "127.0.0.0", Resources.State.HEALTHY),
                RcaTestHelper.generateFlowUnit(
                        type2, "node2", "127.0.0.1", Resources.State.UNHEALTHY));

        flowUnit = clusterRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        clusterSummary = flowUnit.getSummary();
        Assert.assertEquals(2, clusterSummary.getNumOfUnhealthyNodes());
        Assert.assertTrue(
                compareNodeSummary("node1", type1, clusterSummary.getHotNodeSummaryList().get(0)));
        Assert.assertTrue(
                compareNodeSummary("node2", type2, clusterSummary.getHotNodeSummaryList().get(1)));

        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        type1, "node1", "127.0.0.0", Resources.State.HEALTHY),
                RcaTestHelper.generateFlowUnit(
                        type1, "node2", "127.0.0.1", Resources.State.HEALTHY));

        nodeRca2.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        type2, "node1", "127.0.0.0", Resources.State.HEALTHY),
                RcaTestHelper.generateFlowUnit(
                        type2, "node2", "127.0.0.1", Resources.State.UNHEALTHY));

        flowUnit = clusterRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        clusterSummary = flowUnit.getSummary();
        Assert.assertEquals(1, clusterSummary.getNumOfUnhealthyNodes());
        Assert.assertTrue(
                compareNodeSummary("node2", type2, clusterSummary.getHotNodeSummaryList().get(0)));
    }

    @Test
    public void testTableEntryExpire() {
        Clock constantClock = Clock.fixed(ofEpochMilli(0), ZoneId.systemDefault());
        ResourceFlowUnit<HotClusterSummary> flowUnit;

        clusterRca.setClock(constantClock);
        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        type1, "node1", "127.0.0.0", Resources.State.UNHEALTHY, 0));
        flowUnit = clusterRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        Assert.assertEquals(1, flowUnit.getSummary().getNumOfUnhealthyNodes());

        clusterRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(3)));
        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        type2,
                        "node2",
                        "127.0.0.1",
                        Resources.State.UNHEALTHY,
                        TimeUnit.MINUTES.toMillis(3)));
        flowUnit = clusterRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        Assert.assertEquals(2, flowUnit.getSummary().getNumOfUnhealthyNodes());

        clusterRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(11)));
        nodeRca.mockFlowUnit();
        flowUnit = clusterRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        Assert.assertEquals(1, flowUnit.getSummary().getNumOfUnhealthyNodes());

        clusterRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(14)));
        nodeRca.mockFlowUnit();
        flowUnit = clusterRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isHealthy());
    }

    @Test
    public void testCollectFromClusterManagerNode() {
        ResourceFlowUnit<HotClusterSummary> flowUnit;
        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        type1, "cluster_manager", "127.0.0.9", Resources.State.UNHEALTHY));
        flowUnit = clusterRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isHealthy());

        clusterRca.setCollectFromClusterManagerNode(true);
        nodeRca.mockFlowUnit();
        flowUnit = clusterRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isHealthy());

        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        type1, "cluster_manager", "127.0.0.9", Resources.State.UNHEALTHY));
        flowUnit = clusterRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        Assert.assertEquals(1, flowUnit.getSummary().getNumOfUnhealthyNodes());
        Assert.assertEquals(4, flowUnit.getSummary().getNumOfNodes());
        Assert.assertTrue(
                compareNodeSummary(
                        "cluster_manager",
                        type1,
                        flowUnit.getSummary().getHotNodeSummaryList().get(0)));
    }

    @Test
    public void testRemoveNodeFromCluster() throws SQLException, ClassNotFoundException {
        ResourceFlowUnit<HotClusterSummary> flowUnit;
        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        type1, "node1", "127.0.0.0", Resources.State.UNHEALTHY));
        flowUnit = clusterRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());

        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        type2, "node2", "127.0.0.1", Resources.State.UNHEALTHY));
        flowUnit = clusterRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        Assert.assertEquals(2, flowUnit.getSummary().getNumOfUnhealthyNodes());

        ClusterDetailsEventProcessor clusterDetailsEventProcessor = removeNodeFromCluster();
        appContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);

        nodeRca.mockFlowUnit();
        flowUnit = clusterRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        Assert.assertEquals(1, flowUnit.getSummary().getNumOfUnhealthyNodes());
        Assert.assertTrue(
                compareNodeSummary(
                        "node2", type2, flowUnit.getSummary().getHotNodeSummaryList().get(0)));
    }

    @Test
    public void testAddNewNodeIntoCluster() throws SQLException, ClassNotFoundException {
        ResourceFlowUnit<HotClusterSummary> flowUnit;
        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        type1, "node1", "127.0.0.0", Resources.State.UNHEALTHY));
        flowUnit = clusterRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());

        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        type2, "node4", "127.0.0.3", Resources.State.UNHEALTHY));
        flowUnit = clusterRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        Assert.assertEquals(1, flowUnit.getSummary().getNumOfUnhealthyNodes());
        Assert.assertTrue(
                compareNodeSummary(
                        "node1", type1, flowUnit.getSummary().getHotNodeSummaryList().get(0)));

        ClusterDetailsEventProcessor clusterDetailsEventProcessor = addNewNodeIntoCluster();

        nodeRca.mockFlowUnit();
        appContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);
        flowUnit = clusterRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        Assert.assertEquals(1, flowUnit.getSummary().getNumOfUnhealthyNodes());
        Assert.assertTrue(
                compareNodeSummary(
                        "node1", type1, flowUnit.getSummary().getHotNodeSummaryList().get(0)));

        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        type2, "node4", "127.0.0.3", Resources.State.UNHEALTHY));
        flowUnit = clusterRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        HotClusterSummary clusterSummary = flowUnit.getSummary();
        Assert.assertEquals(2, clusterSummary.getNumOfUnhealthyNodes());
        Assert.assertTrue(
                compareNodeSummary("node1", type1, clusterSummary.getHotNodeSummaryList().get(0)));
        Assert.assertTrue(
                compareNodeSummary("node4", type2, clusterSummary.getHotNodeSummaryList().get(1)));
    }

    private ClusterDetailsEventProcessor removeNodeFromCluster()
            throws SQLException, ClassNotFoundException {
        ClusterDetailsEventProcessorTestHelper clusterDetailsEventProcessorTestHelper =
                new ClusterDetailsEventProcessorTestHelper();
        clusterDetailsEventProcessorTestHelper.addNodeDetails("node2", "127.0.0.1", false);
        clusterDetailsEventProcessorTestHelper.addNodeDetails("node3", "127.0.0.2", false);
        clusterDetailsEventProcessorTestHelper.addNodeDetails(
                "cluster_manager", "127.0.0.9", AllMetrics.NodeRole.ELECTED_CLUSTER_MANAGER, true);
        return clusterDetailsEventProcessorTestHelper.generateClusterDetailsEvent();
    }

    private ClusterDetailsEventProcessor addNewNodeIntoCluster()
            throws SQLException, ClassNotFoundException {
        ClusterDetailsEventProcessorTestHelper clusterDetailsEventProcessorTestHelper =
                new ClusterDetailsEventProcessorTestHelper();
        clusterDetailsEventProcessorTestHelper.addNodeDetails("node1", "127.0.0.0", false);
        clusterDetailsEventProcessorTestHelper.addNodeDetails("node2", "127.0.0.1", false);
        clusterDetailsEventProcessorTestHelper.addNodeDetails("node3", "127.0.0.2", false);
        clusterDetailsEventProcessorTestHelper.addNodeDetails("node4", "127.0.0.3", false);
        clusterDetailsEventProcessorTestHelper.addNodeDetails(
                "cluster_manager", "127.0.0.9", AllMetrics.NodeRole.ELECTED_CLUSTER_MANAGER, true);
        return clusterDetailsEventProcessorTestHelper.generateClusterDetailsEvent();
    }

    private boolean compareResourceSummary(Resource resource, HotResourceSummary resourceSummary) {
        return resourceSummary.getResource().equals(resource);
    }

    private boolean compareNodeSummary(
            String nodeId, Resource resource, HotNodeSummary nodeSummary) {
        if (!nodeId.equals(nodeSummary.getNodeID().toString())) {
            return false;
        }
        if (nodeSummary.getHotResourceSummaryList().isEmpty()) {
            return false;
        }
        return compareResourceSummary(resource, nodeSummary.getHotResourceSummaryList().get(0));
    }
}
