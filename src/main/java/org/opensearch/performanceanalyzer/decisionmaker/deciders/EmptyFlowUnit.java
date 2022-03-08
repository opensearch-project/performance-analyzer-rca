/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders;


import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;

public class EmptyFlowUnit extends GenericFlowUnit {

    public EmptyFlowUnit(long timeStamp) {
        super(timeStamp);
    }

    @Override
    public FlowUnitMessage buildFlowUnitMessage(String graphNode, InstanceDetails.Id node) {
        throw new IllegalStateException(
                this.getClass().getSimpleName() + " not expected to be passed over wire");
    }
}
