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
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
