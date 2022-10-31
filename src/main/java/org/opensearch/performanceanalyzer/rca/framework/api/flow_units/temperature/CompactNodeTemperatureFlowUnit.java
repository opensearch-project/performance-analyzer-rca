/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.flow_units.temperature;


import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.temperature.CompactNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;

/**
 * This is a grpc wrapper on top of the CompactNodeTemperatureSummary. The flow unit is passed
 * around from the data nodes to the master. Its compact, because it does not contain the
 * temperatures at the granularity of shards. As, some of our largest instances can have multiple
 * shards, it would be sending too many bytes over the wire.
 */
public class CompactNodeTemperatureFlowUnit extends ResourceFlowUnit<CompactNodeSummary> {
    private final CompactNodeSummary compactNodeTemperatureSummary;

    public CompactNodeTemperatureFlowUnit(
            long timeStamp,
            ResourceContext context,
            CompactNodeSummary resourceSummary,
            boolean persistSummary) {
        super(timeStamp, context, resourceSummary, persistSummary);
        this.compactNodeTemperatureSummary = resourceSummary;
    }

    public CompactNodeTemperatureFlowUnit(long timeStamp) {
        super(timeStamp);
        compactNodeTemperatureSummary = null;
    }

    @Override
    public FlowUnitMessage buildFlowUnitMessage(String graphNode, InstanceDetails.Id node) {
        FlowUnitMessage.Builder builder = FlowUnitMessage.newBuilder();
        builder.setGraphNode(graphNode);
        builder.setNode(node.toString());
        builder.setTimeStamp(System.currentTimeMillis());
        if (compactNodeTemperatureSummary != null) {
            compactNodeTemperatureSummary.buildSummaryMessageAndAddToFlowUnit(builder);
        }
        return builder.build();
    }

    public static CompactNodeTemperatureFlowUnit buildFlowUnitFromWrapper(
            final FlowUnitMessage message) {
        CompactNodeSummary compactNodeTemperatureSummary =
                CompactNodeSummary.buildNodeTemperatureProfileFromMessage(
                        message.getNodeTemperatureSummary());
        return new CompactNodeTemperatureFlowUnit(
                message.getTimeStamp(),
                new ResourceContext(Resources.State.UNKNOWN),
                compactNodeTemperatureSummary,
                false);
    }

    public CompactNodeSummary getCompactNodeTemperatureSummary() {
        return compactNodeTemperatureSummary;
    }
}
