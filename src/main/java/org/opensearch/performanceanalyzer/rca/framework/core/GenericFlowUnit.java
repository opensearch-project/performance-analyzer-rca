/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.core;

import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;

// TODO: Doc comments and a description of each member.
public abstract class GenericFlowUnit {

    private long timeStamp;
    protected boolean empty;

    // Creates an empty flow unit.
    public GenericFlowUnit(long timeStamp) {
        this.empty = true;
        this.timeStamp = timeStamp;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public boolean isEmpty() {
        return this.empty;
    }

    public abstract FlowUnitMessage buildFlowUnitMessage(
            final String graphNode, final InstanceDetails.Id node);
}
