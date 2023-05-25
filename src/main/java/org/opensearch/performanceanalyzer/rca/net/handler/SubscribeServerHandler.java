/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.net.handler;


import io.grpc.stub.StreamObserver;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.commons.collectors.StatsCollector;
import org.opensearch.performanceanalyzer.commons.stats.CommonStats;
import org.opensearch.performanceanalyzer.commons.stats.metrics.StatExceptionCode;
import org.opensearch.performanceanalyzer.grpc.SubscribeMessage;
import org.opensearch.performanceanalyzer.grpc.SubscribeResponse;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaGraphMetrics;
import org.opensearch.performanceanalyzer.rca.net.SubscriptionManager;
import org.opensearch.performanceanalyzer.rca.net.requests.CompositeSubscribeRequest;
import org.opensearch.performanceanalyzer.rca.net.tasks.SubscriptionRxTask;

/** Service handler for the subscribe RPC */
public class SubscribeServerHandler {

    private static final Logger LOG = LogManager.getLogger(SubscribeServerHandler.class);
    private final AtomicReference<ExecutorService> executorServiceAtomicReference;
    private final SubscriptionManager subscriptionManager;

    public SubscribeServerHandler(
            final SubscriptionManager subscriptionManager,
            final AtomicReference<ExecutorService> executorServiceAtomicReference) {
        this.executorServiceAtomicReference = executorServiceAtomicReference;
        this.subscriptionManager = subscriptionManager;
    }

    public void handleSubscriptionRequest(
            final SubscribeMessage request,
            final StreamObserver<SubscribeResponse> responseObserver) {
        final CompositeSubscribeRequest subscribeRequest =
                new CompositeSubscribeRequest(request, responseObserver);
        final ExecutorService executorService = executorServiceAtomicReference.get();
        if (executorService != null) {
            try {
                executorService.execute(
                        new SubscriptionRxTask(subscriptionManager, subscribeRequest));
                CommonStats.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                        RcaGraphMetrics.NET_BYTES_IN,
                        subscribeRequest.getSubscribeMessage().getRequesterGraphNode(),
                        subscribeRequest.getSubscribeMessage().getSerializedSize());
            } catch (final RejectedExecutionException ree) {
                LOG.warn(
                        "Dropped processing subscription request because the network threadpool is full");
                StatsCollector.instance()
                        .logException(StatExceptionCode.RCA_NETWORK_THREADPOOL_QUEUE_FULL_ERROR);
            }
        }
    }
}
