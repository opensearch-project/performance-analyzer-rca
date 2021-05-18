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
