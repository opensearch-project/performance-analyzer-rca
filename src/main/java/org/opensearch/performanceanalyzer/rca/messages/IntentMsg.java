/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.messages;

import java.util.Map;

public class IntentMsg {
    /**
     * The node sending the intent. This is the node whose one or more dependency is not locally
     * available.
     */
    String requesterGraphNode;

    /** The name of the destination node whose data is desired. */
    String destinationGraphNode;

    /**
     * The requesting node's rca.conf tags. This tags will be used by the requested Node's network
     * thread to send data.
     */
    Map<String, String> rcaConfTags;

    public String getRequesterGraphNode() {
        return requesterGraphNode;
    }

    public String getDestinationGraphNode() {
        return destinationGraphNode;
    }

    public Map<String, String> getRcaConfTags() {
        return rcaConfTags;
    }

    public IntentMsg(
            String requesterGraphNode,
            String destinationGraphNode,
            Map<String, String> rcaConfTags) {
        this.requesterGraphNode = requesterGraphNode;
        this.destinationGraphNode = destinationGraphNode;
        this.rcaConfTags = rcaConfTags;
    }

    @Override
    public String toString() {
        return String.format(
                "Intent::from: '%s', to: '%s'", requesterGraphNode, destinationGraphNode);
    }
}
