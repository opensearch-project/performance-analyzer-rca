/*
 * Copyright 2020-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazon.opendistro.opensearch.performanceanalyzer.decisionmaker.actions;


import com.amazon.opendistro.opensearch.performanceanalyzer.AppContext;
import com.amazon.opendistro.opensearch.performanceanalyzer.metrics.AllMetrics;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;
import com.amazon.opendistro.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
        ClusterDetailsEventProcessor.NodeDetails master =
                new ClusterDetailsEventProcessor.NodeDetails(
                        AllMetrics.NodeRole.ELECTED_MASTER, "master", "127.0.0.3", true);

        List<ClusterDetailsEventProcessor.NodeDetails> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        nodes.add(master);
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
