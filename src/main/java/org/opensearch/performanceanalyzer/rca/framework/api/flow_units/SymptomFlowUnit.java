/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.flow_units;

import java.util.List;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.SymptomContext;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;

public class SymptomFlowUnit extends GenericFlowUnit {

    private SymptomContext context = null;
    private List<List<String>> data;

    public SymptomFlowUnit(long timeStamp) {
        super(timeStamp);
    }

    public SymptomFlowUnit(long timeStamp, SymptomContext context) {
        super(timeStamp);
        this.context = context;
    }

    public SymptomFlowUnit(long timeStamp, List<List<String>> data, SymptomContext context) {
        super(timeStamp);
        this.context = context;
        this.data = data;
    }

    public SymptomContext getContext() {
        return this.context;
    }

    public List<List<String>> getData() {
        return this.data;
    }

    public static SymptomFlowUnit generic() {
        return new SymptomFlowUnit(System.currentTimeMillis());
    }

    public FlowUnitMessage buildFlowUnitMessage(
            final String graphNode, final InstanceDetails.Id node) {
        final FlowUnitMessage.Builder messageBuilder = FlowUnitMessage.newBuilder();
        messageBuilder.setGraphNode(graphNode);
        messageBuilder.setNode(node.toString());

        messageBuilder.setTimeStamp(System.currentTimeMillis());

        return messageBuilder.build();
    }

    @Override
    public String toString() {
        return String.format("%d", this.getTimeStamp());
    }
}
