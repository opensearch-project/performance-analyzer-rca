/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.jvmsizing;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;

public class LargeHeapClusterRca extends Rca<ResourceFlowUnit<HotClusterSummary>> {

    private static final long EVAL_INTERVAL_IN_S = 5;

    private final Rca<ResourceFlowUnit<HotNodeSummary>> oldGenContendedRca;

    public LargeHeapClusterRca(final Rca<ResourceFlowUnit<HotNodeSummary>> oldGenContendedRca) {
        super(EVAL_INTERVAL_IN_S);
        this.oldGenContendedRca = oldGenContendedRca;
    }

    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        throw new UnsupportedOperationException(
                "generateFlowUnitListFromWire is not supported on the"
                        + " node-local RCA: "
                        + args.getNode().name());
    }

    @Override
    public ResourceFlowUnit<HotClusterSummary> operate() {
        List<ResourceFlowUnit<HotNodeSummary>> oldGenContendedFlowUnits =
                oldGenContendedRca.getFlowUnits();
        List<HotNodeSummary> unhealthyNodeSummaries = new ArrayList<>();
        long currTime = System.currentTimeMillis();
        for (ResourceFlowUnit<HotNodeSummary> flowUnit : oldGenContendedFlowUnits) {
            if (flowUnit.isEmpty()) {
                continue;
            }

            if (flowUnit.getResourceContext().isUnhealthy()) {
                unhealthyNodeSummaries.add(flowUnit.getSummary());
            }
        }

        if (unhealthyNodeSummaries.isEmpty()) {
            return new ResourceFlowUnit<>(currTime);
        }

        final HotClusterSummary summary =
                new HotClusterSummary(
                        getAppContext().getAllClusterInstances().size(),
                        unhealthyNodeSummaries.stream()
                                .map(HotNodeSummary::getNodeID)
                                .collect(Collectors.toSet())
                                .size());
        for (HotNodeSummary hotNodeSummary : unhealthyNodeSummaries) {
            summary.appendNestedSummary(hotNodeSummary);
        }

        final ResourceContext context = new ResourceContext(Resources.State.CONTENDED);

        return new ResourceFlowUnit<>(currTime, context, summary);
    }
}
