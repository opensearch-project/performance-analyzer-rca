/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 * Copyright 2019-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package org.opensearch.performanceanalyzer.store.rca;

import static java.time.Instant.ofEpochMilli;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.GradleTaskForRca;
import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.RcaTestHelper;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.store.rca.HotNodeClusterRca;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;

@Category(GradleTaskForRca.class)
public class HotNodeClusterRcaTest {
    private AppContext appContext;

    @Before
    public void setupCluster() {
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

        List<ClusterDetailsEventProcessor.NodeDetails> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        nodes.add(node3);
        clusterDetailsEventProcessor.setNodesDetails(nodes);

        appContext = new AppContext();
        appContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);
    }

    @Test
    public void testNodeCntThresholdAndTimestampExpiration() {
        RcaTestHelper nodeRca = new RcaTestHelper();
        nodeRca.setAppContext(appContext);
        HotNodeClusterRcaX clusterRca = new HotNodeClusterRcaX(1, nodeRca);
        clusterRca.setAppContext(appContext);

        Clock constantClock = Clock.fixed(ofEpochMilli(0), ZoneId.systemDefault());
        clusterRca.setClock(constantClock);

        nodeRca.mockFlowUnit(generateFlowUnit(ResourceUtil.OLD_GEN_HEAP_USAGE, 2, "node1"));
        // did not collect enough nodes
        Assert.assertFalse(clusterRca.operate().getResourceContext().isUnhealthy());

        clusterRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(6)));
        nodeRca.mockFlowUnit(generateFlowUnit(ResourceUtil.OLD_GEN_HEAP_USAGE, 8, "node2"));
        // first node expires
        Assert.assertFalse(clusterRca.operate().getResourceContext().isUnhealthy());

        nodeRca.mockFlowUnit(generateFlowUnit(ResourceUtil.OLD_GEN_HEAP_USAGE, 2, "node1"));
        Assert.assertTrue(clusterRca.operate().getResourceContext().isUnhealthy());
    }

    @Test
    public void testCaptureHotNode() {
        ResourceFlowUnit fu;
        RcaTestHelper nodeRca = new RcaTestHelper();
        nodeRca.setAppContext(appContext);
        HotNodeClusterRcaX clusterRca = new HotNodeClusterRcaX(1, nodeRca);
        clusterRca.setAppContext(appContext);

        // medium = 5, below the 30% threshold
        nodeRca.mockFlowUnit(generateFlowUnit(ResourceUtil.OLD_GEN_HEAP_USAGE, 4, "node1"));
        Assert.assertFalse(clusterRca.operate().getResourceContext().isUnhealthy());
        nodeRca.mockFlowUnit(generateFlowUnit(ResourceUtil.OLD_GEN_HEAP_USAGE, 5, "node2"));
        Assert.assertFalse(clusterRca.operate().getResourceContext().isUnhealthy());
        nodeRca.mockFlowUnit(generateFlowUnit(ResourceUtil.OLD_GEN_HEAP_USAGE, 6, "node3"));
        Assert.assertFalse(clusterRca.operate().getResourceContext().isUnhealthy());

        // 10 is above 5*1.3
        nodeRca.mockFlowUnit(generateFlowUnit(ResourceUtil.OLD_GEN_HEAP_USAGE, 10, "node1"));
        fu = clusterRca.operate();
        Assert.assertTrue(fu.getResourceContext().isUnhealthy());
        Assert.assertTrue(fu.hasResourceSummary());
        HotClusterSummary clusterSummary = (HotClusterSummary) fu.getSummary();
        Assert.assertTrue(clusterSummary.getNumOfUnhealthyNodes() == 1);
        Assert.assertTrue(clusterSummary.getNestedSummaryList().size() > 0);

        HotNodeSummary nodeSummary = (HotNodeSummary) clusterSummary.getNestedSummaryList().get(0);
        Assert.assertTrue(nodeSummary.getNodeID().toString().equals("node1"));
        Assert.assertTrue(nodeSummary.getNestedSummaryList().size() > 0);

        HotResourceSummary resourceSummary =
                (HotResourceSummary) nodeSummary.getNestedSummaryList().get(0);
        Assert.assertTrue(resourceSummary.getResource().equals(ResourceUtil.OLD_GEN_HEAP_USAGE));
        Assert.assertEquals(resourceSummary.getValue(), 10, 0.1);
    }

    @Test
    // check whether can filter out noise data if the resource usage is very small
    public void testFilterNoiseData() {
        RcaTestHelper nodeRca = new RcaTestHelper();
        nodeRca.setAppContext(appContext);
        HotNodeClusterRcaX clusterRca = new HotNodeClusterRcaX(1, nodeRca);
        clusterRca.setAppContext(appContext);

        // medium = 0.2, 0.8 is above the 30% threshold. but since the data is too small, we will
        // drop it
        nodeRca.mockFlowUnit(generateFlowUnit(ResourceUtil.OLD_GEN_HEAP_USAGE, 0.1, "node1"));
        Assert.assertFalse(clusterRca.operate().getResourceContext().isUnhealthy());
        nodeRca.mockFlowUnit(generateFlowUnit(ResourceUtil.OLD_GEN_HEAP_USAGE, 0.2, "node2"));
        Assert.assertFalse(clusterRca.operate().getResourceContext().isUnhealthy());
        nodeRca.mockFlowUnit(generateFlowUnit(ResourceUtil.OLD_GEN_HEAP_USAGE, 0.8, "node3"));
        Assert.assertFalse(clusterRca.operate().getResourceContext().isUnhealthy());
    }

    private static class HotNodeClusterRcaX extends HotNodeClusterRca {
        public <R extends Rca> HotNodeClusterRcaX(final int rcaPeriod, final R hotNodeRca) {
            super(rcaPeriod, hotNodeRca);
        }

        public void setClock(Clock testClock) {
            this.clock = testClock;
        }
    }

    private ResourceFlowUnit generateFlowUnit(Resource type, double val, String nodeId) {
        HotResourceSummary resourceSummary = new HotResourceSummary(type, 10, val, 60);
        HotNodeSummary nodeSummary =
                new HotNodeSummary(
                        new InstanceDetails.Id(nodeId), new InstanceDetails.Ip("127.0.0.0"));
        nodeSummary.appendNestedSummary(resourceSummary);
        return new ResourceFlowUnit(
                System.currentTimeMillis(),
                new ResourceContext(Resources.State.HEALTHY),
                nodeSummary);
    }
}
