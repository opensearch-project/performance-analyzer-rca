/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.flow_units.temperature;


import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.temperature.ClusterTemperatureSummary;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;

public class ClusterTemperatureFlowUnit extends ResourceFlowUnit<ClusterTemperatureSummary> {
    private final ClusterTemperatureSummary clusterTemperatureSummary;

    public ClusterTemperatureFlowUnit(
            long timeStamp, ResourceContext context, ClusterTemperatureSummary resourceSummary) {
        super(timeStamp, context, resourceSummary, true);
        clusterTemperatureSummary = resourceSummary;
    }

    public ClusterTemperatureFlowUnit(long timeStamp) {
        super(timeStamp);
        clusterTemperatureSummary = null;
    }

    @Override
    public FlowUnitMessage buildFlowUnitMessage(String graphNode, InstanceDetails.Id node) {
        throw new IllegalStateException(
                this.getClass().getSimpleName() + " should not be passed " + "over the wire.");
    }

    public ClusterTemperatureSummary getClusterTemperatureSummary() {
        return clusterTemperatureSummary;
    }
}
