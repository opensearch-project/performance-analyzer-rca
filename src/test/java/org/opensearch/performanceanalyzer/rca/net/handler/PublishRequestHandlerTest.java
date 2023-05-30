/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.net.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.grpc.PublishResponse;
import org.opensearch.performanceanalyzer.rca.net.NodeStateManager;
import org.opensearch.performanceanalyzer.rca.net.ReceivedFlowUnitStore;
import org.opensearch.performanceanalyzer.rca.net.tasks.FlowUnitRxTask;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PowerMockIgnore({"org.apache.logging.log4j.*", "com.sun.org.apache.xerces.*"})
@RunWith(PowerMockRunner.class)
@PrepareForTest({AtomicReference.class, PublishRequestHandler.class})
public class PublishRequestHandlerTest {

    private static final List<String> DUMMY_GRAPH_NODES =
            Arrays.asList("dummyNode1", "dummyNode2", "dummyNode3");

    @Mock private NodeStateManager nodeStateManager;
    @Mock private ReceivedFlowUnitStore receivedFlowUnitStore;
    private AtomicReference<ExecutorService> atomicReference;
    @Mock private ExecutorService executorService;
    @Mock private StreamObserver<PublishResponse> serviceResponse;
    private PublishRequestHandler publishRequestHandler;

    @Before
    public void setup() {
        initMocks(this);
        PerformanceAnalyzerApp.initAggregators();
        atomicReference =
                (AtomicReference<ExecutorService>) PowerMockito.mock(AtomicReference.class);
        PowerMockito.when(atomicReference.get()).thenReturn(executorService);
        doNothing().when(executorService).execute(any(FlowUnitRxTask.class));
        this.publishRequestHandler =
                new PublishRequestHandler(nodeStateManager, receivedFlowUnitStore, atomicReference);
    }

    @Test
    public void testGetClientStreamWithSampleInputs() {
        List<StreamObserver<FlowUnitMessage>> flowUnitList = getClientStreamTestData(2, 5);

        DUMMY_GRAPH_NODES.forEach(
                node -> {
                    FlowUnitMessage flowUnitMessage =
                            FlowUnitMessage.newBuilder().setGraphNode(node).build();
                    flowUnitList.forEach(
                            flowUnitMessageStreamObserver -> {
                                flowUnitMessageStreamObserver.onNext(flowUnitMessage);
                            });
                });

        verify(executorService, times(DUMMY_GRAPH_NODES.size() * flowUnitList.size()))
                .execute(any(FlowUnitRxTask.class));
    }

    @Test
    public void testTerminateUpstreamConnsWithFewResponseStreamCompleted() {
        List<StreamObserver<FlowUnitMessage>> flowUnitList = getClientStreamTestData(3, 6);
        AtomicInteger numberOfStreamCompleted = new AtomicInteger();
        flowUnitList.forEach(
                flowUnitMessageStreamObserver -> {
                    if (new Random().nextBoolean()) {
                        flowUnitMessageStreamObserver.onCompleted();
                        numberOfStreamCompleted.getAndIncrement();
                    }
                });
        // Verify that till now onNext on stream was invoked for this many times.
        verify(serviceResponse, times(numberOfStreamCompleted.get()))
                .onNext(any(PublishResponse.class));
        publishRequestHandler.terminateUpstreamConnections();
        // Verify that now onNext on stream should have been called for total number of response
        // stream present.
        verify(serviceResponse, times(flowUnitList.size())).onNext(any(PublishResponse.class));
        Assert.assertTrue(publishRequestHandler.getDataClientStreamList().isEmpty());
    }

    private List<StreamObserver<FlowUnitMessage>> getClientStreamTestData(
            int lowerBound, int upperBound) {
        int randomNumber =
                new Random().nextInt((upperBound - lowerBound + 1))
                        + lowerBound; // [5, 10] Upper bound included.
        List<StreamObserver<FlowUnitMessage>> flowUnitList = new ArrayList<>();
        for (int iterator = 1; iterator <= randomNumber; iterator++) {
            flowUnitList.add(publishRequestHandler.getClientStream(serviceResponse));
        }
        return flowUnitList;
    }
}
