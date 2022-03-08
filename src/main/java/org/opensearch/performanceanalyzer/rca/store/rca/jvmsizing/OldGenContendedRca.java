/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.jvmsizing;


import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.rca.configs.OldGenContendedRcaConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaVerticesMetrics;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;
import org.opensearch.performanceanalyzer.rca.util.MemInfoParser;

public class OldGenContendedRca extends Rca<ResourceFlowUnit<HotNodeSummary>> {

    private static final Logger LOG = LogManager.getLogger(OldGenContendedRca.class);
    private static final long GB_TO_B = 1024 * 1024 * 1024;
    private static final long EVAL_INTERVAL_IN_S = 5;
    private Rca<ResourceFlowUnit<HotResourceSummary>> highOldGenOccupancyRca;
    private Rca<ResourceFlowUnit<HotResourceSummary>> oldGenReclamationRca;
    private int minTotalMemoryThresholdInGB =
            OldGenContendedRcaConfig.DEFAULT_MIN_TOTAL_MEMORY_IN_GB;
    private final long totalMemory;

    public OldGenContendedRca(
            final Rca<ResourceFlowUnit<HotResourceSummary>> highOldGenOccupancyRca,
            final Rca<ResourceFlowUnit<HotResourceSummary>> oldGenReclamationRca) {
        super(EVAL_INTERVAL_IN_S);
        this.highOldGenOccupancyRca = highOldGenOccupancyRca;
        this.oldGenReclamationRca = oldGenReclamationRca;
        this.totalMemory = MemInfoParser.getTotalMemory();
    }

    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        List<FlowUnitMessage> flowUnitMessages = args.getWireHopper().readFromWire(args.getNode());
        setFlowUnits(
                flowUnitMessages.stream()
                        .map(
                                (Function<FlowUnitMessage, ResourceFlowUnit<HotNodeSummary>>)
                                        ResourceFlowUnit::buildFlowUnitFromWrapper)
                        .collect(Collectors.toList()));
    }

    @Override
    public ResourceFlowUnit<HotNodeSummary> operate() {
        List<ResourceFlowUnit<HotResourceSummary>> oldGenOccupancyFlowUnits =
                highOldGenOccupancyRca.getFlowUnits();
        List<ResourceFlowUnit<HotResourceSummary>> oldGenReclamationFlowUnits =
                oldGenReclamationRca.getFlowUnits();
        long currTime = System.currentTimeMillis();

        // we expect only one flow unit to be present for both these RCAs as the nodes are scheduled
        // at the same frequency.

        if (oldGenOccupancyFlowUnits.size() != 1 || oldGenReclamationFlowUnits.size() != 1) {
            LOG.warn(
                    "Was expecting both oldGenOccupancy and oldGenReclamation RCAs to have exactly one"
                            + " flowunit. Found: "
                            + oldGenOccupancyFlowUnits.size()
                            + ", and "
                            + oldGenReclamationFlowUnits.size()
                            + " respectively");
            return new ResourceFlowUnit<>(currTime);
        }

        if (this.totalMemory < minTotalMemoryThresholdInGB * GB_TO_B) {
            return new ResourceFlowUnit<>(currTime);
        }

        ResourceFlowUnit<HotResourceSummary> oldGenOccupancyFlowUnit =
                oldGenOccupancyFlowUnits.get(0);
        ResourceFlowUnit<HotResourceSummary> oldGenReclamationFlowUnit =
                oldGenReclamationFlowUnits.get(0);

        if (!oldGenOccupancyFlowUnit.isEmpty()) {
            boolean isOccupancyUnhealthy =
                    oldGenOccupancyFlowUnit.getResourceContext().isUnhealthy();
            boolean isFullGcIneffective =
                    oldGenReclamationFlowUnit.getResourceContext().isUnhealthy();

            if (isOccupancyUnhealthy && isFullGcIneffective) {
                InstanceDetails instanceDetails = getAppContext().getMyInstanceDetails();
                HotNodeSummary summary =
                        new HotNodeSummary(
                                instanceDetails.getInstanceId(), instanceDetails.getInstanceIp());
                summary.appendNestedSummary(oldGenOccupancyFlowUnit.getSummary());
                summary.appendNestedSummary(oldGenReclamationFlowUnit.getSummary());

                ResourceContext context = new ResourceContext(Resources.State.CONTENDED);
                PerformanceAnalyzerApp.RCA_VERTICES_METRICS_AGGREGATOR.updateStat(
                        RcaVerticesMetrics.OLD_GEN_CONTENDED, "", 1);
                return new ResourceFlowUnit<>(currTime, context, summary);
            }
        }

        return new ResourceFlowUnit<>(currTime);
    }

    @Override
    public void readRcaConf(RcaConf conf) {
        super.readRcaConf(conf);
        readTotalMemThresholdFromConf(conf);
    }

    private void readTotalMemThresholdFromConf(final RcaConf conf) {
        OldGenContendedRcaConfig rcaConfig = conf.getOldGenContendedRcaConfig();
        this.minTotalMemoryThresholdInGB = rcaConfig.getMinTotalMemoryThresholdInGb();
    }
}
