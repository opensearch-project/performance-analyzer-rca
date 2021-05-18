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
