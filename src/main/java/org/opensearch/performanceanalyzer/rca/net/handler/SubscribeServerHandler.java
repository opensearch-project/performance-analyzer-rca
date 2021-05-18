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
 * Copyright 2019-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package org.opensearch.performanceanalyzer.rca.net.handler;


import io.grpc.stub.StreamObserver;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.collectors.StatExceptionCode;
import org.opensearch.performanceanalyzer.collectors.StatsCollector;
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
                PerformanceAnalyzerApp.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
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
