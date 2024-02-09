/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.store.rca;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.GradleTaskForRca;
import org.opensearch.performanceanalyzer.rca.framework.api.RcaTestHelper;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.store.rca.HighHeapUsageClusterRca;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;

@Category(GradleTaskForRca.class)
public class HighHeapUsageClusterRcaTest {

    @Test
    public void testOperate() {
        ClusterDetailsEventProcessor clusterDetailsEventProcessor =
                new ClusterDetailsEventProcessor();
        ClusterDetailsEventProcessor.NodeDetails node1 =
                new ClusterDetailsEventProcessor.NodeDetails(
                        AllMetrics.NodeRole.DATA, "node1", "127.0.0.0", false);
        ClusterDetailsEventProcessor.NodeDetails node2 =
                new ClusterDetailsEventProcessor.NodeDetails(
                        AllMetrics.NodeRole.DATA, "node2", "127.0.0.1", false);

        List<ClusterDetailsEventProcessor.NodeDetails> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        clusterDetailsEventProcessor.setNodesDetails(nodes);

        AppContext appContext = new AppContext();
        appContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);

        RcaTestHelper nodeRca = new RcaTestHelper();
        nodeRca.setAppContext(appContext);

        HighHeapUsageClusterRca clusterRca = new HighHeapUsageClusterRca(1, nodeRca);
        clusterRca.setAppContext(appContext);

        // send three young gen flowunits (healthy, unhealthy, unhealthy) to node1
        // the cluterRca will generate three healthy flowunits
        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        ResourceUtil.YOUNG_GEN_PROMOTION_RATE, "node1", Resources.State.HEALTHY));
        Assert.assertFalse(clusterRca.operate().getResourceContext().isUnhealthy());
        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        ResourceUtil.YOUNG_GEN_PROMOTION_RATE, "node1", Resources.State.UNHEALTHY));
        Assert.assertFalse(clusterRca.operate().getResourceContext().isUnhealthy());
        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        ResourceUtil.YOUNG_GEN_PROMOTION_RATE, "node1", Resources.State.UNHEALTHY));
        Assert.assertFalse(clusterRca.operate().getResourceContext().isUnhealthy());

        // send two young gen flowunits (unhealthy, unhealthy) to node2
        // the cluterRca will continue generating healthy flowunits
        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        ResourceUtil.YOUNG_GEN_PROMOTION_RATE, "node2", Resources.State.UNHEALTHY));
        Assert.assertFalse(clusterRca.operate().getResourceContext().isUnhealthy());
        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        ResourceUtil.YOUNG_GEN_PROMOTION_RATE, "node2", Resources.State.UNHEALTHY));
        Assert.assertFalse(clusterRca.operate().getResourceContext().isUnhealthy());

        // send two old gen flowunits (unhealthy, unhealthy) to node1
        // the cluterRca will continue generating healthy flowunits
        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        ResourceUtil.OLD_GEN_HEAP_USAGE, "node1", Resources.State.UNHEALTHY));
        Assert.assertFalse(clusterRca.operate().getResourceContext().isUnhealthy());
        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        ResourceUtil.OLD_GEN_HEAP_USAGE, "node1", Resources.State.UNHEALTHY));
        Assert.assertFalse(clusterRca.operate().getResourceContext().isUnhealthy());

        // send one old gen flowunits (unhealthy) to node1
        // the cluterRca will generate a unhealthy flowunit at the end
        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        ResourceUtil.OLD_GEN_HEAP_USAGE, "node1", Resources.State.UNHEALTHY));
        Assert.assertTrue(clusterRca.operate().getResourceContext().isUnhealthy());

        // send one young gen flowunits (unhealthy) to node1
        // flowunit becomes healthy
        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        ResourceUtil.YOUNG_GEN_PROMOTION_RATE, "node1", Resources.State.UNHEALTHY));
        Assert.assertFalse(clusterRca.operate().getResourceContext().isUnhealthy());

        // send one old gen flowunits (unhealthy) to node2
        // the cluterRca will generate a unhealthy flowunit at the end
        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        ResourceUtil.YOUNG_GEN_PROMOTION_RATE, "node2", Resources.State.UNHEALTHY));
        Assert.assertTrue(clusterRca.operate().getResourceContext().isUnhealthy());
    }
}
