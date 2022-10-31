/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.RcaTestHelper;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;
import org.opensearch.performanceanalyzer.rca.store.rca.HighHeapUsageClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.QueueRejectionClusterRca;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;

public class QueueHealthDeciderTest {
    private static final JsonParser JSON_PARSER = new JsonParser();
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
        ClusterDetailsEventProcessor.NodeDetails master =
                new ClusterDetailsEventProcessor.NodeDetails(
                        AllMetrics.NodeRole.ELECTED_MASTER, "master", "127.0.0.9", true);

        List<ClusterDetailsEventProcessor.NodeDetails> nodes = new ArrayList<>();
        nodes.add(node1);
        nodes.add(node2);
        nodes.add(node3);
        nodes.add(node4);
        nodes.add(master);
        clusterDetailsEventProcessor.setNodesDetails(nodes);

        appContext = new AppContext();
        appContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);
        String rcaConfPath = Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca.conf").toString();
        rcaConf = new RcaConf(rcaConfPath);
    }

    @Test
    public void testHighRejectionRemediation() {
        RcaTestHelper<HotNodeSummary> nodeRca = new RcaTestHelper<>("QueueRejectionNodeRca");
        nodeRca.setAppContext(appContext);
        // node1: Both write and search queues unhealthy
        // node2: Only write unhealthy
        // node3: Only search unhealthy
        // node4: all queues healthy
        nodeRca.mockFlowUnit(
                RcaTestHelper.generateFlowUnit(
                        "node1",
                        "127.0.0.1",
                        Resources.State.UNHEALTHY,
                        ResourceUtil.WRITE_QUEUE_REJECTION,
                        ResourceUtil.SEARCH_QUEUE_REJECTION),
                RcaTestHelper.generateFlowUnit(
                        "node2",
                        "127.0.0.2",
                        Resources.State.UNHEALTHY,
                        ResourceUtil.WRITE_QUEUE_REJECTION),
                RcaTestHelper.generateFlowUnit(
                        "node3",
                        "127.0.0.3",
                        Resources.State.UNHEALTHY,
                        ResourceUtil.SEARCH_QUEUE_REJECTION),
                RcaTestHelper.generateFlowUnit("node4", "127.0.0.4", Resources.State.HEALTHY));

        appContext
                .getNodeConfigCache()
                .put(
                        new NodeKey(
                                new InstanceDetails.Id("node1"),
                                new InstanceDetails.Ip("127.0.0.1")),
                        ResourceUtil.SEARCH_QUEUE_CAPACITY,
                        5000);
        appContext
                .getNodeConfigCache()
                .put(
                        new NodeKey(
                                new InstanceDetails.Id("node1"),
                                new InstanceDetails.Ip("127.0.0.1")),
                        ResourceUtil.WRITE_QUEUE_CAPACITY,
                        5000);
        appContext
                .getNodeConfigCache()
                .put(
                        new NodeKey(
                                new InstanceDetails.Id("node2"),
                                new InstanceDetails.Ip("127.0.0.2")),
                        ResourceUtil.WRITE_QUEUE_CAPACITY,
                        5000);
        appContext
                .getNodeConfigCache()
                .put(
                        new NodeKey(
                                new InstanceDetails.Id("node3"),
                                new InstanceDetails.Ip("127.0.0.3")),
                        ResourceUtil.SEARCH_QUEUE_CAPACITY,
                        5000);

        QueueRejectionClusterRca queueClusterRca = new QueueRejectionClusterRca(1, nodeRca);
        queueClusterRca.setAppContext(appContext);
        queueClusterRca.generateFlowUnitListFromLocal(null);

        HighHeapUsageClusterRca clusterRca = new HighHeapUsageClusterRca(1, nodeRca);
        QueueHealthDecider decider = new QueueHealthDecider(5, 12, queueClusterRca, clusterRca);
        decider.setAppContext(appContext);
        decider.readRcaConf(rcaConf);

        // Since deciderFrequency is 12, the first 11 invocations return empty decision
        for (int i = 0; i < 11; i++) {
            Decision decision = decider.operate();
            assertTrue(decision.isEmpty());
        }

        Decision decision = decider.operate();
        assertEquals(4, decision.getActions().size());

        Map<String, Map<ResourceEnum, Integer>> nodeActionCounter = new HashMap<>();
        for (Action action : decision.getActions()) {
            assertEquals(1, action.impactedNodes().size());
            String nodeId = action.impactedNodes().get(0).getNodeId().toString();
            String summary = action.summary();
            JsonObject jsonObject = JSON_PARSER.parse(summary).getAsJsonObject();

            if (jsonObject.get("resource").getAsInt()
                    == ResourceEnum.WRITE_THREADPOOL.getNumber()) {
                nodeActionCounter
                        .computeIfAbsent(nodeId, k -> new HashMap<>())
                        .merge(ResourceEnum.WRITE_THREADPOOL, 1, Integer::sum);
            }
            if (jsonObject.get("resource").getAsInt()
                    == ResourceEnum.SEARCH_THREADPOOL.getNumber()) {
                nodeActionCounter
                        .computeIfAbsent(nodeId, k -> new HashMap<>())
                        .merge(ResourceEnum.SEARCH_THREADPOOL, 1, Integer::sum);
            }
        }

        assertEquals(2, nodeActionCounter.get("node1").size());
        assertEquals(1, (int) nodeActionCounter.get("node1").get(ResourceEnum.WRITE_THREADPOOL));
        assertEquals(1, (int) nodeActionCounter.get("node1").get(ResourceEnum.SEARCH_THREADPOOL));
        assertEquals(1, nodeActionCounter.get("node2").size());
        assertEquals(1, (int) nodeActionCounter.get("node2").get(ResourceEnum.WRITE_THREADPOOL));
        assertEquals(1, nodeActionCounter.get("node3").size());
        assertEquals(1, (int) nodeActionCounter.get("node3").get(ResourceEnum.SEARCH_THREADPOOL));
        assertFalse(nodeActionCounter.containsKey("node4"));
    }
}
