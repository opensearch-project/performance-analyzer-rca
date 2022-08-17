/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.actions;


import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;

public class CacheClearActionTest {

    private final AppContext testAppContext;
    private final Set<NodeKey> dataNodeKeySet;

    public CacheClearActionTest() {
        testAppContext = new AppContext();
        dataNodeKeySet = new HashSet<>();
    }

    @Before
    public void setupClusterDetails() {
        ClusterDetailsEventProcessor clusterDetailsEventProcessor =
                new ClusterDetailsEventProcessor();
        ClusterDetailsEventProcessor.NodeDetails node1 =
                new ClusterDetailsEventProcessor.NodeDetails(
                        AllMetrics.NodeRole.DATA, "node1", "127.0.0.0", false);
        ClusterDetailsEventProcessor.NodeDetails node2 =
                new ClusterDetailsEventProcessor.NodeDetails(
                        AllMetrics.NodeRole.DATA, "node2", "127.0.0.1", false);
        ClusterDetailsEventProcessor.NodeDetails cluster_manager =
                new ClusterDetailsEventProcessor.NodeDetails(
                        AllMetrics.NodeRole.ELECTED_CLUSTER_MANAGER,
                        "cluster_manager",
                        "127.0.0.3",
                        true);

        List<ClusterDetailsEventProcessor.NodeDetails> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        nodes.add(cluster_manager);
        clusterDetailsEventProcessor.setNodesDetails(nodes);
        testAppContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);

        dataNodeKeySet.add(
                new NodeKey(
                        new InstanceDetails.Id(node1.getId()),
                        new InstanceDetails.Ip(node1.getHostAddress())));
        dataNodeKeySet.add(
                new NodeKey(
                        new InstanceDetails.Id(node2.getId()),
                        new InstanceDetails.Ip(node2.getHostAddress())));
    }

    @Test
    public void testBuildAction() {
        CacheClearAction cacheClearAction = CacheClearAction.newBuilder(testAppContext).build();
        Assert.assertTrue(cacheClearAction.isActionable());
        Assert.assertEquals(
                CacheClearAction.Builder.DEFAULT_COOL_OFF_PERIOD_IN_MILLIS,
                cacheClearAction.coolOffPeriodInMillis());
        List<NodeKey> impactedNode = cacheClearAction.impactedNodes();
        Assert.assertEquals(2, impactedNode.size());
        for (NodeKey node : impactedNode) {
            Assert.assertTrue(dataNodeKeySet.contains(node));
        }
        Map<NodeKey, ImpactVector> impactVectorMap = cacheClearAction.impact();
        Assert.assertEquals(2, impactVectorMap.size());
        for (Map.Entry<NodeKey, ImpactVector> entry : impactVectorMap.entrySet()) {
            Assert.assertTrue(dataNodeKeySet.contains(entry.getKey()));
            Map<ImpactVector.Dimension, ImpactVector.Impact> impact = entry.getValue().getImpact();
            Assert.assertEquals(
                    ImpactVector.Impact.DECREASES_PRESSURE,
                    impact.get(ImpactVector.Dimension.HEAP));
            Assert.assertEquals(
                    ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.CPU));
            Assert.assertEquals(
                    ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.NETWORK));
            Assert.assertEquals(
                    ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.RAM));
            Assert.assertEquals(
                    ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.DISK));
        }
    }

    @Test
    public void testMutedAction() {
        testAppContext.updateMutedActions(ImmutableSet.of(CacheClearAction.NAME));
        CacheClearAction cacheClearAction = CacheClearAction.newBuilder(testAppContext).build();
        Assert.assertFalse(cacheClearAction.isActionable());
    }
}
