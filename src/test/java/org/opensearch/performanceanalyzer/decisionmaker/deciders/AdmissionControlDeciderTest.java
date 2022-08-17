/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders;

import static org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil.HEAP_MAX_SIZE;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.core.util.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.RcaTestHelper;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;
import org.opensearch.performanceanalyzer.rca.store.rca.admissioncontrol.AdmissionControlClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;

public class AdmissionControlDeciderTest {

    AppContext appContext;
    RcaConf rcaConf;

    @Before
    public void setupCluster() {
        ClusterDetailsEventProcessor clusterDetailsEventProcessor =
                new ClusterDetailsEventProcessor();

        ClusterDetailsEventProcessor.NodeDetails node1 =
                new ClusterDetailsEventProcessor.NodeDetails(
                        AllMetrics.NodeRole.DATA, "node1", "127.0.0.1", false);
        ClusterDetailsEventProcessor.NodeDetails node2 =
                new ClusterDetailsEventProcessor.NodeDetails(
                        AllMetrics.NodeRole.DATA, "node2", "127.0.0.2", false);
        ClusterDetailsEventProcessor.NodeDetails node3 =
                new ClusterDetailsEventProcessor.NodeDetails(
                        AllMetrics.NodeRole.DATA, "node3", "127.0.0.3", false);
        ClusterDetailsEventProcessor.NodeDetails node4 =
                new ClusterDetailsEventProcessor.NodeDetails(
                        AllMetrics.NodeRole.DATA, "node3", "127.0.0.4", false);
        ClusterDetailsEventProcessor.NodeDetails clusterManager =
                new ClusterDetailsEventProcessor.NodeDetails(
                        AllMetrics.NodeRole.ELECTED_CLUSTER_MANAGER,
                        "cluster_manager",
                        "127.0.0.9",
                        true);

        List<ClusterDetailsEventProcessor.NodeDetails> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        nodes.add(node3);
        nodes.add(node4);
        nodes.add(clusterManager);
        clusterDetailsEventProcessor.setNodesDetails(nodes);

        appContext = new AppContext();
        appContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);

        String rcaConfPath = Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca.conf").toString();
        rcaConf = new RcaConf(rcaConfPath);
    }

    @Test
    public void testAdmissionControlDecider() {
        RcaTestHelper<HotNodeSummary> admissionControlNodeRca =
                new RcaTestHelper<>("AdmissionControlNodeRca");
        admissionControlNodeRca.setAppContext(this.appContext);

        AdmissionControlClusterRca admissionControlClusterRca =
                new AdmissionControlClusterRca(1, admissionControlNodeRca);
        admissionControlClusterRca.setAppContext(this.appContext);

        AdmissionControlDecider admissionControlDecider =
                new AdmissionControlDecider(1, 1, admissionControlClusterRca);
        admissionControlDecider.setAppContext(this.appContext);
        admissionControlDecider.readRcaConf(this.rcaConf);

        ResourceContext resourceContext = new ResourceContext(Resources.State.UNHEALTHY);
        NodeKey nodeKey =
                new NodeKey(new InstanceDetails.Id("node1"), new InstanceDetails.Ip("127.0.0.1"));
        HotResourceSummary resourceSummary = new HotResourceSummary(HEAP_MAX_SIZE, 1.0, 1.0, 0);
        HotNodeSummary nodeSummary =
                new HotNodeSummary(nodeKey.getNodeId(), nodeKey.getHostAddress());
        nodeSummary.appendNestedSummary(resourceSummary);
        HotClusterSummary clusterSummary = new HotClusterSummary(5, 1);
        clusterSummary.appendNestedSummary(nodeSummary);

        admissionControlClusterRca.setLocalFlowUnit(
                new ResourceFlowUnit<>(
                        System.currentTimeMillis(), resourceContext, clusterSummary, true));

        Decision decision = admissionControlDecider.operate();
        Assert.isNonEmpty(decision);
    }
}
