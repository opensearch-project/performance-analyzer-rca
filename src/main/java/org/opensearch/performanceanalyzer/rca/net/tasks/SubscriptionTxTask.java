/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.net.tasks;


import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.grpc.SubscribeMessage;
import org.opensearch.performanceanalyzer.net.NetClient;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaGraphMetrics;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.messages.IntentMsg;
import org.opensearch.performanceanalyzer.rca.net.NodeStateManager;
import org.opensearch.performanceanalyzer.rca.net.SubscribeResponseHandler;
import org.opensearch.performanceanalyzer.rca.net.SubscriptionManager;

/** Task to send out a subscription request. */
public abstract class SubscriptionTxTask implements Runnable {

    private static final Logger LOG = LogManager.getLogger(SubscriptionTxTask.class);

    /** The client object to make RPC with. */
    protected final NetClient netClient;

    /** The encapsulated subscribe message. */
    protected final IntentMsg intentMsg;

    /** The subscription manager to update metadata. */
    protected final SubscriptionManager subscriptionManager;

    /** The node state manager to start/update tracking staleness. */
    protected final NodeStateManager nodeStateManager;

    private final AppContext appContext;

    public SubscriptionTxTask(
            final NetClient netClient,
            final IntentMsg intentMsg,
            final SubscriptionManager subscriptionManager,
            final NodeStateManager nodeStateManager,
            final AppContext appContext) {
        this.netClient = netClient;
        this.intentMsg = intentMsg;
        this.subscriptionManager = subscriptionManager;
        this.nodeStateManager = nodeStateManager;
        this.appContext = appContext;
    }

    protected void sendSubscribeRequest(
            final InstanceDetails remoteHost,
            final String requesterVertex,
            final String destinationVertex,
            final Map<String, String> tags) {
        LOG.debug("rca: [sub-tx]: {} -> {} to {}", requesterVertex, destinationVertex, remoteHost);
        final SubscribeMessage subscribeMessage =
                SubscribeMessage.newBuilder()
                        .setDestinationGraphNode(destinationVertex)
                        .setRequesterGraphNode(requesterVertex)
                        .putTags("locus", tags.get("locus"))
                        .putTags(
                                "requester",
                                appContext.getMyInstanceDetails().getInstanceId().toString())
                        .build();
        netClient.subscribe(
                remoteHost,
                subscribeMessage,
                new SubscribeResponseHandler(
                        subscriptionManager, nodeStateManager, remoteHost, destinationVertex));
        PerformanceAnalyzerApp.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                RcaGraphMetrics.RCA_NODES_SUB_REQ_COUNT,
                requesterVertex + ":" + destinationVertex,
                1);
    }

    protected Set<InstanceDetails> getPeerInstances() {
        return appContext.getPeerInstances();
    }
}
