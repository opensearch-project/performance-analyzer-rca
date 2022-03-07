/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.net.tasks;


import io.grpc.stub.StreamObserver;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.grpc.SubscribeMessage;
import org.opensearch.performanceanalyzer.grpc.SubscribeResponse;
import org.opensearch.performanceanalyzer.grpc.SubscribeResponse.SubscriptionStatus;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaGraphMetrics;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.net.SubscriptionManager;
import org.opensearch.performanceanalyzer.rca.net.requests.CompositeSubscribeRequest;

/** Task that processes received subscribe messages. */
public class SubscriptionRxTask implements Runnable {

    private static final Logger LOG = LogManager.getLogger(SubscriptionRxTask.class);

    /** The subscription manager instance to update metadata. */
    private final SubscriptionManager subscriptionManager;

    /** The subscribe message with the response stream. */
    private final CompositeSubscribeRequest compositeSubscribeRequest;

    public SubscriptionRxTask(
            final SubscriptionManager subscriptionManager,
            final CompositeSubscribeRequest compositeSubscribeRequest) {
        this.subscriptionManager = subscriptionManager;
        this.compositeSubscribeRequest = compositeSubscribeRequest;
    }

    /**
     * Process the subscription request.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        final SubscribeMessage request = compositeSubscribeRequest.getSubscribeMessage();
        final Map<String, String> tags = request.getTagsMap();
        final InstanceDetails.Id requesterHostId =
                new InstanceDetails.Id(tags.getOrDefault("requester", ""));
        final String locus = tags.getOrDefault("locus", "");
        final SubscriptionStatus subscriptionStatus =
                subscriptionManager.addSubscriber(
                        request.getDestinationGraphNode(), requesterHostId, locus);

        LOG.debug(
                "rca: [sub-rx]: {} <- {} from {} Result: {}",
                request.getDestinationGraphNode(),
                request.getRequesterGraphNode(),
                requesterHostId,
                subscriptionStatus);

        final StreamObserver<SubscribeResponse> responseStream =
                compositeSubscribeRequest.getSubscribeResponseStream();
        // TODO: Wrap this in a try-catch
        responseStream.onNext(
                SubscribeResponse.newBuilder().setSubscriptionStatus(subscriptionStatus).build());
        responseStream.onCompleted();
        PerformanceAnalyzerApp.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                RcaGraphMetrics.RCA_NODES_SUB_ACK_COUNT,
                request.getRequesterGraphNode() + ":" + request.getDestinationGraphNode(),
                1);
    }
}
