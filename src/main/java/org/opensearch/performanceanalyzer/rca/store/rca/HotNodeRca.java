/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca;


import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;

public class HotNodeRca extends Rca<ResourceFlowUnit<HotNodeSummary>> {

    private static final Logger LOG = LogManager.getLogger(HotNodeRca.class);
    private Rca<ResourceFlowUnit<HotResourceSummary>>[] hotResourceRcas;
    private boolean hasUnhealthyFlowUnit;
    // the amount of RCA period this RCA needs to run before sending out a flowunit
    private final int rcaPeriod;
    private int counter;

    public <R extends Rca<ResourceFlowUnit<HotResourceSummary>>> HotNodeRca(
            final int rcaPeriod, R... hotResourceRcas) {
        super(5);
        this.hotResourceRcas = hotResourceRcas.clone();
        this.rcaPeriod = rcaPeriod;
        this.counter = 0;
        hasUnhealthyFlowUnit = false;
    }

    @Override
    public ResourceFlowUnit<HotNodeSummary> operate() {
        counter++;
        List<HotResourceSummary> hotResourceSummaryList = new ArrayList<>();
        for (int i = 0; i < hotResourceRcas.length; i++) {
            final List<ResourceFlowUnit<HotResourceSummary>> hotResourceFlowUnits =
                    hotResourceRcas[i].getFlowUnits();
            for (final ResourceFlowUnit<HotResourceSummary> hotResourceFlowUnit :
                    hotResourceFlowUnits) {
                if (hotResourceFlowUnit.isEmpty()) {
                    continue;
                }
                if (hotResourceFlowUnit.hasResourceSummary()) {
                    hotResourceSummaryList.add(hotResourceFlowUnit.getSummary());
                }
                if (hotResourceFlowUnit.getResourceContext().isUnhealthy()) {
                    hasUnhealthyFlowUnit = true;
                }
            }
        }

        if (counter == rcaPeriod) {
            ResourceContext context;

            InstanceDetails instanceDetails = getInstanceDetails();
            HotNodeSummary summary =
                    new HotNodeSummary(
                            instanceDetails.getInstanceId(), instanceDetails.getInstanceIp());

            for (HotResourceSummary hotResourceSummary : hotResourceSummaryList) {
                summary.appendNestedSummary(hotResourceSummary);
            }

            if (hasUnhealthyFlowUnit) {
                context = new ResourceContext(Resources.State.UNHEALTHY);
            } else {
                context = new ResourceContext(Resources.State.HEALTHY);
            }

            // reset the variables
            counter = 0;
            hasUnhealthyFlowUnit = false;
            // check if the current node is data node. If it is the data node
            // then HotNodeRca is the top level RCA on this node and we want to persist summaries in
            // flowunit.
            boolean isDataNode = !instanceDetails.getIsMaster();
            return new ResourceFlowUnit<>(System.currentTimeMillis(), context, summary, isDataNode);
        } else {
            return new ResourceFlowUnit<>(System.currentTimeMillis());
        }
    }

    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        final List<FlowUnitMessage> flowUnitMessages =
                args.getWireHopper().readFromWire(args.getNode());
        List<ResourceFlowUnit<HotNodeSummary>> flowUnitList = new ArrayList<>();
        LOG.debug("rca: Executing fromWire: {}", this.getClass().getSimpleName());
        for (FlowUnitMessage flowUnitMessage : flowUnitMessages) {
            flowUnitList.add(ResourceFlowUnit.buildFlowUnitFromWrapper(flowUnitMessage));
        }
        setFlowUnits(flowUnitList);
    }
}
