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
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package org.opensearch.performanceanalyzer.net;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.grpc.stub.StreamObserver;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.grpc.InterNodeRpcServiceGrpc;
import org.opensearch.performanceanalyzer.grpc.PublishResponse;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PowerMockIgnore({"org.apache.logging.log4j.*", "com.sun.org.apache.xerces.*"})
@RunWith(PowerMockRunner.class)
@PrepareForTest({NetClient.class, InterNodeRpcServiceGrpc.InterNodeRpcServiceStub.class})
public class NetClientTest {

    private static final List<String> DUMMY_GRAPH_NODES =
            Arrays.asList("dummyNode1", "dummyNode2", "dummyNode3");

    @Mock private GRPCConnectionManager connectionManager;
    @Mock private StreamObserver<PublishResponse> publishResponseStreamObserver;
    @Mock private StreamObserver<FlowUnitMessage> streamObserver;
    private InterNodeRpcServiceGrpc.InterNodeRpcServiceStub stub;
    private NetClient netClient;

    @Before
    public void setup() {
        initMocks(this);
        this.stub =
                PowerMockito.mock(
                        InterNodeRpcServiceGrpc.InterNodeRpcServiceStub
                                .class); // Mocking final class
        this.netClient = new NetClient(connectionManager);
    }

    @Test
    public void testPublishWithDifferentNode() {
        InstanceDetails dummyRemoteHost = new InstanceDetails(AllMetrics.NodeRole.ELECTED_MASTER);

        DUMMY_GRAPH_NODES.forEach(
                node -> {
                    FlowUnitMessage flowUnitMessage =
                            FlowUnitMessage.newBuilder().setGraphNode(node).build();
                    mockForPublish(dummyRemoteHost, flowUnitMessage);
                    netClient.publish(
                            dummyRemoteHost, flowUnitMessage, publishResponseStreamObserver);
                });

        verify(connectionManager, times(DUMMY_GRAPH_NODES.size()))
                .getClientStubForHost(any(InstanceDetails.class));

        Assert.assertNotNull(
                netClient
                        .getPerHostAndNodeOpenDataStreamMap()
                        .get(dummyRemoteHost.getInstanceId()));
        // Assert that data stream map contains all the expected graph nodes.
        Assert.assertEquals(
                DUMMY_GRAPH_NODES.size(),
                netClient
                        .getPerHostAndNodeOpenDataStreamMap()
                        .get(dummyRemoteHost.getInstanceId())
                        .size());
        DUMMY_GRAPH_NODES.forEach(
                expectedGraph -> {
                    Assert.assertTrue(
                            netClient
                                    .getPerHostAndNodeOpenDataStreamMap()
                                    .get(dummyRemoteHost.getInstanceId())
                                    .containsKey(expectedGraph));
                });
    }

    private void mockForPublish(InstanceDetails instanceDetails, FlowUnitMessage flowUnitMessage) {
        when(connectionManager.getClientStubForHost(instanceDetails)).thenReturn(stub);
        PowerMockito.when(stub.publish(publishResponseStreamObserver)).thenReturn(streamObserver);
        doNothing().when(streamObserver).onNext(flowUnitMessage);
    }
}
