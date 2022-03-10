/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.net.tasks;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.rca.GradleTaskForRca;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.net.NodeStateManager;
import org.opensearch.performanceanalyzer.rca.net.ReceivedFlowUnitStore;

@Category(GradleTaskForRca.class)
public class FlowUnitRxTaskTest {

    private static final String TEST_GRAPH_NODE = "testGraphNode";
    private static final String TEST_OPEN_SEARCH_NODE = "testOpenSearchNode";

    private FlowUnitRxTask testFlowUnitRxTask;
    private FlowUnitMessage testFlowUnitMessage =
            FlowUnitMessage.newBuilder()
                    .setGraphNode(TEST_GRAPH_NODE)
                    .setNode(TEST_OPEN_SEARCH_NODE)
                    .build();

    @Mock private NodeStateManager mockNodeStateManager;

    @Mock private ReceivedFlowUnitStore mockReceivedFlowUnitStore;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        testFlowUnitRxTask =
                new FlowUnitRxTask(
                        mockNodeStateManager, mockReceivedFlowUnitStore, testFlowUnitMessage);
    }

    @Test
    public void testEnqueueSuccess() {
        when(mockReceivedFlowUnitStore.enqueue(TEST_GRAPH_NODE, testFlowUnitMessage))
                .thenReturn(true);

        testFlowUnitRxTask.run();

        verify(mockNodeStateManager)
                .updateReceiveTime(
                        eq(new InstanceDetails.Id(TEST_OPEN_SEARCH_NODE)),
                        eq(TEST_GRAPH_NODE),
                        anyLong());
    }
}
