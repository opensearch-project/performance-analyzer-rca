/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.messages;

import java.util.Map;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;

public class UnicastIntentMsg extends IntentMsg {

    private final InstanceDetails unicastDestinationInstance;

    public UnicastIntentMsg(
            String requesterNode,
            String destinationNode,
            Map<String, String> rcaConfTags,
            InstanceDetails unicastDestinationInstance) {
        super(requesterNode, destinationNode, rcaConfTags);
        this.unicastDestinationInstance = unicastDestinationInstance;
    }

    public InstanceDetails getUnicastDestinationInstance() {
        return unicastDestinationInstance;
    }
}
