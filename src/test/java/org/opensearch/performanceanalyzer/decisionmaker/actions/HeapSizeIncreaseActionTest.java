/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HeapSizeIncreaseAction.class, HeapSizeIncreaseActionTest.class})
public class HeapSizeIncreaseActionTest {
    @Mock private AppContext mockAppContext;

    @Mock private Runtime mockRuntime;

    private HeapSizeIncreaseAction testAction;

    private String selfId = "node1";
    private String selfIp = "1.2.3.4";
    private static final int TEST_PORT = 1;
    private NodeKey myNodeKey;

    @Before
    public void setUp() {
        initMocks(this);
        mockStatic(Runtime.class);
        myNodeKey = getNodeKeyFor(selfId, selfIp);
        Mockito.when(mockAppContext.getMyInstanceDetails())
                .thenReturn(
                        new InstanceDetails(
                                myNodeKey.getNodeId(), myNodeKey.getHostAddress(), TEST_PORT));
        testAction = new HeapSizeIncreaseAction(mockAppContext);
    }

    @Test
    public void testImpactedNodes() throws Exception {
        NodeKey dataNode = getNodeKeyFor("node1", "2.3.4.5");
        Mockito.when(mockAppContext.getDataNodeInstances())
                .thenReturn(
                        Collections.singletonList(
                                new InstanceDetails(
                                        dataNode.getNodeId(),
                                        dataNode.getHostAddress(),
                                        TEST_PORT)));

        List<NodeKey> impactedNodes = testAction.impactedNodes();
        assertEquals(1, impactedNodes.size());
        assertEquals(dataNode.getNodeId(), impactedNodes.get(0).getNodeId());
        assertEquals(dataNode.getHostAddress(), impactedNodes.get(0).getHostAddress());
    }

    @Test
    public void testImpact() throws Exception {
        NodeKey dataNode = getNodeKeyFor("node1", "2.3.4.5");
        Mockito.when(mockAppContext.getDataNodeInstances())
                .thenReturn(
                        Collections.singletonList(
                                new InstanceDetails(
                                        dataNode.getNodeId(),
                                        dataNode.getHostAddress(),
                                        TEST_PORT)));

        Map<NodeKey, ImpactVector> impactMap = testAction.impact();
        Set<NodeKey> nodeKeys = impactMap.keySet();

        assertEquals(1, nodeKeys.size());
        for (NodeKey nodeKey : nodeKeys) {
            ImpactVector vector = impactMap.get(nodeKey);
            assertEquals(dataNode.getNodeId(), nodeKey.getNodeId());
            assertEquals(dataNode.getHostAddress(), nodeKey.getHostAddress());
            Assert.assertEquals(
                    ImpactVector.Impact.DECREASES_PRESSURE,
                    vector.getImpact().get(ImpactVector.Dimension.HEAP));
        }
    }

    @Test
    public void testSummary() {
        String summaryStr = testAction.summary();
        JsonObject summaryObj = JsonParser.parseString(summaryStr).getAsJsonObject();
        String nodeId = summaryObj.get("Id").getAsString();
        String nodeIp = summaryObj.get("Ip").getAsString();
        assertEquals(selfId, nodeId);
        assertEquals(selfIp, nodeIp);

        HeapSizeIncreaseAction rebuiltAction =
                HeapSizeIncreaseAction.fromSummary(summaryStr, mockAppContext);
        assertTrue(rebuiltAction.canUpdate());
    }

    private NodeKey getNodeKeyFor(String id, String ip) {
        return new NodeKey(new InstanceDetails.Id(id), new InstanceDetails.Ip(ip));
    }
}
