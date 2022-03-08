/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.flow_units;


import org.junit.Assert;
import org.junit.Test;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.grpc.HotNodeSummaryMessage;
import org.opensearch.performanceanalyzer.grpc.HotResourceSummaryMessage;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

public class NodeConfigFlowUnitTest {
    private long ts = 1234;
    private NodeKey nodeKey =
            new NodeKey(new InstanceDetails.Id("node1"), new InstanceDetails.Ip("127.0.0.1"));

    @Test
    public void testBuildEmptyFlowUnit() {
        NodeConfigFlowUnit flowUnit = new NodeConfigFlowUnit(ts);
        Assert.assertTrue(flowUnit.isEmpty());
        Assert.assertEquals(ts, flowUnit.getTimeStamp());
    }

    @Test
    public void testSetAndReadNodeConfig() {
        NodeConfigFlowUnit flowUnit = new NodeConfigFlowUnit(ts, nodeKey);
        flowUnit.addConfig(ResourceUtil.SEARCH_QUEUE_CAPACITY, 1500);
        Assert.assertFalse(flowUnit.isEmpty());
        Assert.assertEquals(ts, flowUnit.getTimeStamp());
        Assert.assertTrue(flowUnit.hasResourceSummary());
        Assert.assertFalse(flowUnit.isSummaryPersistable());

        Assert.assertTrue(flowUnit.hasConfig(ResourceUtil.SEARCH_QUEUE_CAPACITY));
        Assert.assertFalse(flowUnit.hasConfig(ResourceUtil.FIELD_DATA_CACHE_MAX_SIZE));

        Assert.assertEquals(1500, flowUnit.readConfig(ResourceUtil.SEARCH_QUEUE_CAPACITY), 0.01);

        // node summary
        HotNodeSummary nodeSummary = flowUnit.getSummary();
        Assert.assertEquals(nodeKey.getNodeId(), nodeSummary.getNodeID());
        Assert.assertEquals(nodeKey.getHostAddress(), nodeSummary.getHostAddress());

        // resource summary
        Assert.assertEquals(0, nodeSummary.getHotResourceSummaryList().size());
        HotResourceSummary writeQueueSummary =
                new HotResourceSummary(ResourceUtil.WRITE_QUEUE_CAPACITY, Double.NaN, 30, 0);
        flowUnit.addConfig(writeQueueSummary);
        Assert.assertEquals(0, nodeSummary.getHotResourceSummaryList().size());
        Assert.assertEquals(30, flowUnit.readConfig(ResourceUtil.WRITE_QUEUE_CAPACITY), 0.01);
    }

    @Test
    public void testProtobufMessageSerialization() {
        NodeConfigFlowUnit flowUnit = new NodeConfigFlowUnit(ts, nodeKey);
        flowUnit.addConfig(ResourceUtil.SEARCH_QUEUE_CAPACITY, 1500);
        String graphNode = "TestNode";
        FlowUnitMessage flowUnitMessage =
                flowUnit.buildFlowUnitMessage(graphNode, nodeKey.getNodeId());
        Assert.assertTrue(flowUnitMessage.hasHotNodeSummary());
        Assert.assertFalse(flowUnitMessage.hasHotResourceSummary());
        Assert.assertEquals(nodeKey.getNodeId().toString(), flowUnitMessage.getNode());
        Assert.assertEquals(graphNode, flowUnitMessage.getGraphNode());

        HotNodeSummaryMessage nodeSummaryMessage = flowUnitMessage.getHotNodeSummary();
        Assert.assertTrue(nodeSummaryMessage.hasHotResourceSummaryList());
        Assert.assertEquals(
                1, nodeSummaryMessage.getHotResourceSummaryList().getHotResourceSummaryCount());

        HotResourceSummaryMessage resourceSummaryMessage =
                nodeSummaryMessage.getHotResourceSummaryList().getHotResourceSummary(0);
        Assert.assertEquals(
                ResourceUtil.SEARCH_QUEUE_CAPACITY, resourceSummaryMessage.getResource());
    }

    @Test
    public void testProtobufMessageDeserialization() {
        NodeConfigFlowUnit flowUnit = new NodeConfigFlowUnit(ts, nodeKey);
        flowUnit.addConfig(ResourceUtil.SEARCH_QUEUE_CAPACITY, 1500);
        String graphNode = "TestNode";
        FlowUnitMessage flowUnitMessage =
                flowUnit.buildFlowUnitMessage(graphNode, nodeKey.getNodeId());
        // de-serialize
        NodeConfigFlowUnit flowUnitFromRPC =
                NodeConfigFlowUnit.buildFlowUnitFromWrapper(flowUnitMessage);
        Assert.assertTrue(flowUnitFromRPC.hasConfig(ResourceUtil.SEARCH_QUEUE_CAPACITY));
        Assert.assertFalse(flowUnitFromRPC.hasConfig(ResourceUtil.FIELD_DATA_CACHE_MAX_SIZE));
        Assert.assertEquals(
                1500, flowUnitFromRPC.readConfig(ResourceUtil.SEARCH_QUEUE_CAPACITY), 0.01);
    }
}
