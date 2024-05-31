/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.net.handler;

import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.commons.collectors.StatsCollector;
import org.opensearch.performanceanalyzer.commons.stats.ServiceMetrics;
import org.opensearch.performanceanalyzer.commons.stats.metrics.StatExceptionCode;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.grpc.PublishResponse;
import org.opensearch.performanceanalyzer.grpc.PublishResponse.PublishResponseStatus;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaGraphMetrics;
import org.opensearch.performanceanalyzer.rca.net.NodeStateManager;
import org.opensearch.performanceanalyzer.rca.net.ReceivedFlowUnitStore;
import org.opensearch.performanceanalyzer.rca.net.tasks.FlowUnitRxTask;

/** Service handler for the /sendData RPC. */
public class PublishRequestHandler {

    private static final Logger LOG = LogManager.getLogger(PublishRequestHandler.class);
    private final AtomicReference<ExecutorService> executorReference;
    private final NodeStateManager nodeStateManager;
    private final ReceivedFlowUnitStore receivedFlowUnitStore;
    private List<SendDataClientStreamUpdateConsumer> dataClientStreamList =
            Collections.synchronizedList(new ArrayList<>());

    public PublishRequestHandler(
            NodeStateManager nodeStateManager,
            ReceivedFlowUnitStore receivedFlowUnitStore,
            final AtomicReference<ExecutorService> executorReference) {
        this.executorReference = executorReference;
        this.nodeStateManager = nodeStateManager;
        this.receivedFlowUnitStore = receivedFlowUnitStore;
    }

    public StreamObserver<FlowUnitMessage> getClientStream(
            final StreamObserver<PublishResponse> serviceResponse) {
        SendDataClientStreamUpdateConsumer streamUpdateConsumer =
                new SendDataClientStreamUpdateConsumer(serviceResponse);
        dataClientStreamList.add(streamUpdateConsumer);
        return streamUpdateConsumer;
    }

    public void terminateUpstreamConnections() {
        for (final SendDataClientStreamUpdateConsumer streamUpdateConsumer : dataClientStreamList) {
            StreamObserver<PublishResponse> responseStream =
                    streamUpdateConsumer.getServiceResponse();
            // Check whether the desired stream was already completed by the client before invoking
            // onNext.
            // In case stream was closed/completed by client, calling onNext will result in an
            // exception.
            if (!streamUpdateConsumer.isCompleted()) {
                responseStream.onNext(
                        PublishResponse.newBuilder()
                                .setDataStatus(PublishResponseStatus.NODE_SHUTDOWN)
                                .build());
                responseStream.onCompleted();
            }
        }
        dataClientStreamList.clear();
    }

    // Visible for testing
    protected List<SendDataClientStreamUpdateConsumer> getDataClientStreamList() {
        return this.dataClientStreamList;
    }

    protected class SendDataClientStreamUpdateConsumer implements StreamObserver<FlowUnitMessage> {

        private final StreamObserver<PublishResponse> serviceResponse;
        private boolean isCompleted;

        SendDataClientStreamUpdateConsumer(final StreamObserver<PublishResponse> serviceResponse) {
            this.serviceResponse = serviceResponse;
        }

        public StreamObserver<PublishResponse> getServiceResponse() {
            return this.serviceResponse;
        }

        /**
         * Persist the flow unit sent by the client.
         *
         * @param flowUnitMessage The flow unit that the client just streamed to the server.
         */
        @Override
        public void onNext(FlowUnitMessage flowUnitMessage) {
            final ExecutorService executorService = executorReference.get();
            if (executorService != null) {
                try {
                    executorService.execute(
                            new FlowUnitRxTask(
                                    nodeStateManager, receivedFlowUnitStore, flowUnitMessage));
                    ServiceMetrics.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                            RcaGraphMetrics.NET_BYTES_IN,
                            flowUnitMessage.getGraphNode(),
                            flowUnitMessage.getSerializedSize());
                } catch (final RejectedExecutionException ree) {
                    LOG.warn(
                            "Dropped handling received flow unit because the netwwork threadpool queue is "
                                    + "full");
                    StatsCollector.instance()
                            .logException(
                                    StatExceptionCode.RCA_NETWORK_THREADPOOL_QUEUE_FULL_ERROR);
                }
            }
        }

        /**
         * Client ran into an error while streaming FlowUnits.
         *
         * @param throwable The exception/error that the client encountered.
         */
        @Override
        public void onError(Throwable throwable) {
            LOG.error("Client ran into an error while streaming flow units:", throwable);
        }

        @Override
        public void onCompleted() {
            LOG.debug("Client finished streaming flow units");
            serviceResponse.onNext(buildDataResponse(PublishResponseStatus.SUCCESS));
            serviceResponse.onCompleted();
            this.isCompleted = true;
        }

        public boolean isCompleted() {
            return this.isCompleted;
        }

        private PublishResponse buildDataResponse(final PublishResponseStatus status) {
            return PublishResponse.newBuilder().setDataStatus(status).build();
        }
    }
}
