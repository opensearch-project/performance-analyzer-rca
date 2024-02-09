/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.flow_units.temperature;

import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.temperature.NodeLevelDimensionalSummary;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;

/**
 * This is the FlowUnit wrapper over the summary of the given node across all the tracked dimension.
 * The graph nodes, between which this FlowUnit is passed are local to an OpenSearch node (or in
 * other words, are not transferred over the wire). Therefore, the protobuf message generation is
 * not really required.
 */
public class DimensionalTemperatureFlowUnit extends ResourceFlowUnit<NodeLevelDimensionalSummary> {
    private final NodeLevelDimensionalSummary nodeDimensionProfile;

    public DimensionalTemperatureFlowUnit(
            long timeStamp, final NodeLevelDimensionalSummary nodeDimensionProfile) {
        super(timeStamp, ResourceContext.generic(), nodeDimensionProfile, true);
        this.nodeDimensionProfile = nodeDimensionProfile;
    }

    public DimensionalTemperatureFlowUnit(long timestamp) {
        super(timestamp);
        nodeDimensionProfile = null;
    }

    // A dimension flow unit never leaves a node. So, we don't need to generate protobuf messages.
    @Override
    public FlowUnitMessage buildFlowUnitMessage(String graphNode, InstanceDetails.Id node) {
        throw new IllegalStateException(
                this.getClass().getSimpleName() + " should not be passed " + "over the wire.");
    }

    public NodeLevelDimensionalSummary getNodeDimensionProfile() {
        return nodeDimensionProfile;
    }
}
