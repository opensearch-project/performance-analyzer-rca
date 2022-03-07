/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.net.tasks;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.grpc.stub.StreamObserver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.opensearch.performanceanalyzer.grpc.SubscribeMessage;
import org.opensearch.performanceanalyzer.grpc.SubscribeResponse;
import org.opensearch.performanceanalyzer.rca.GradleTaskForRca;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.net.SubscriptionManager;
import org.opensearch.performanceanalyzer.rca.net.requests.CompositeSubscribeRequest;

@Category(GradleTaskForRca.class)
public class SubscriptionRxTaskTest {

    private static final String TEST_GRAPH_NODE = "testGraphNode";
    private static final String TEST_HOST_ADDRESS = "testHostAddress";
    private static final String TEST_LOCUS = "testLocus";
    private SubscriptionRxTask testSubscriptionRxTask;

    @Mock private SubscriptionManager mockSubscriptionManager;

    @Mock private CompositeSubscribeRequest mockRequest;

    @Mock private StreamObserver<SubscribeResponse> mockResponseStream;

    @Captor private ArgumentCaptor<SubscribeResponse> argCaptor;

    @Before
    public void setUp() {
        initMocks(this);
        testSubscriptionRxTask = new SubscriptionRxTask(mockSubscriptionManager, mockRequest);
    }

    @Test
    public void testSubscribeSuccess() {
        when(mockRequest.getSubscribeMessage()).thenReturn(buildTestSubscribeMessage());
        when(mockSubscriptionManager.addSubscriber(
                        TEST_GRAPH_NODE, new InstanceDetails.Id(TEST_HOST_ADDRESS), TEST_LOCUS))
                .thenReturn(SubscribeResponse.SubscriptionStatus.SUCCESS);
        when(mockRequest.getSubscribeResponseStream()).thenReturn(mockResponseStream);

        testSubscriptionRxTask.run();
        verify(mockResponseStream, times(1)).onNext(argCaptor.capture());
        verify(mockResponseStream, times(1)).onCompleted();

        Assert.assertEquals(
                SubscribeResponse.SubscriptionStatus.SUCCESS,
                argCaptor.getValue().getSubscriptionStatus());
    }

    private SubscribeMessage buildTestSubscribeMessage() {
        return SubscribeMessage.newBuilder()
                .setDestinationGraphNode(TEST_GRAPH_NODE)
                .putTags("locus", TEST_LOCUS)
                .putTags("requester", TEST_HOST_ADDRESS)
                .build();
    }
}
