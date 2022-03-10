/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.net.tasks;


import java.util.Map;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.net.NetClient;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.messages.UnicastIntentMsg;
import org.opensearch.performanceanalyzer.rca.net.NodeStateManager;
import org.opensearch.performanceanalyzer.rca.net.SubscriptionManager;

/** Task that sends out one subscription request to a remote host. */
public class UnicastSubscriptionTxTask extends SubscriptionTxTask {

    /** The host address of the destination node. */
    private final InstanceDetails destinationInstance;

    public UnicastSubscriptionTxTask(
            NetClient netClient,
            UnicastIntentMsg intentMsg,
            SubscriptionManager subscriptionManager,
            NodeStateManager nodeStateManager,
            final AppContext appContext) {
        super(netClient, intentMsg, subscriptionManager, nodeStateManager, appContext);
        this.destinationInstance = intentMsg.getUnicastDestinationInstance();
    }

    /**
     * Sends a subscription request to a known destination address.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        final String requesterVertex = intentMsg.getRequesterGraphNode();
        final String destinationVertex = intentMsg.getDestinationGraphNode();
        final Map<String, String> tags = intentMsg.getRcaConfTags();

        sendSubscribeRequest(destinationInstance, requesterVertex, destinationVertex, tags);
    }
}
