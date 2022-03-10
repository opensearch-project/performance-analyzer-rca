/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.net.tasks;


import java.util.Map;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.net.NetClient;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.messages.IntentMsg;
import org.opensearch.performanceanalyzer.rca.net.NodeStateManager;
import org.opensearch.performanceanalyzer.rca.net.SubscriptionManager;

/** Task that broadcasts a subscription request to the current node's peers. */
public class BroadcastSubscriptionTxTask extends SubscriptionTxTask {
    public BroadcastSubscriptionTxTask(
            NetClient netClient,
            IntentMsg intentMsg,
            SubscriptionManager subscriptionManager,
            NodeStateManager nodeStateManager,
            final AppContext appContext) {
        super(netClient, intentMsg, subscriptionManager, nodeStateManager, appContext);
    }

    /**
     * Broadcasts a subscription request to all the peers in the cluster.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        final String requesterVertex = intentMsg.getRequesterGraphNode();
        final String destinationVertex = intentMsg.getDestinationGraphNode();
        final Map<String, String> tags = intentMsg.getRcaConfTags();

        for (final InstanceDetails remoteHost : getPeerInstances()) {
            sendSubscribeRequest(remoteHost, requesterVertex, destinationVertex, tags);
        }
    }
}
