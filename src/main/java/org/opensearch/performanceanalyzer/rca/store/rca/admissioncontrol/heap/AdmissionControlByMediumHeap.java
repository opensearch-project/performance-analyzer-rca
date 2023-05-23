/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.admissioncontrol.heap;

import static org.opensearch.performanceanalyzer.rca.framework.api.Resources.State.HEALTHY;
import static org.opensearch.performanceanalyzer.rca.framework.api.Resources.State.UNHEALTHY;
import static org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil.HEAP_MAX_SIZE;
import static org.opensearch.performanceanalyzer.rca.framework.metrics.RcaVerticesMetrics.ADMISSION_CONTROL_RCA_TRIGGERED;

import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.commons.stats.CommonStats;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.store.rca.admissioncontrol.model.HeapMetric;
import org.opensearch.performanceanalyzer.util.range.Range;
import org.opensearch.performanceanalyzer.util.range.RangeConfiguration;

/** AdmissionControl RCA for heap > 4gb and <= 32gb */
public class AdmissionControlByMediumHeap implements AdmissionControlByHeap {

    private static final Logger LOG = LogManager.getLogger(AdmissionControlByMediumHeap.class);
    private double previousHeapPercent = 0;

    private InstanceDetails instanceDetails;
    private RangeConfiguration requestSizeHeapRange;

    @Override
    public void init(InstanceDetails instanceDetails, RangeConfiguration rangeConfiguration) {
        this.instanceDetails = instanceDetails;
        this.requestSizeHeapRange = rangeConfiguration;
    }

    @Override
    public ResourceFlowUnit<HotNodeSummary> generateFlowUnits(HeapMetric heapMetric) {

        long currentTimeMillis = System.currentTimeMillis();
        double currentHeapPercent = heapMetric.getHeapPercent();

        HotNodeSummary nodeSummary =
                new HotNodeSummary(
                        instanceDetails.getInstanceId(), instanceDetails.getInstanceIp());

        // If we observe heap percent range change then we tune request-size controller threshold
        // by marking resource as unhealthy and setting desired value as configured
        if (requestSizeHeapRange.hasRangeChanged(previousHeapPercent, currentHeapPercent)) {

            double currentThreshold = getThreshold(requestSizeHeapRange, currentHeapPercent);
            if (currentThreshold == 0) {
                // AdmissionControl rejects all requests if threshold is set to 0, thus ignoring
                return new ResourceFlowUnit<>(
                        currentTimeMillis,
                        new ResourceContext(HEALTHY),
                        nodeSummary,
                        !instanceDetails.getIsClusterManager());
            }

            LOG.debug(
                    "[AdmissionControl] PreviousHeapPercent={} CurrentHeapPercent={} CurrentDesiredThreshold={}",
                    previousHeapPercent,
                    currentHeapPercent,
                    currentThreshold);

            double previousThreshold = getThreshold(requestSizeHeapRange, previousHeapPercent);
            previousHeapPercent = currentHeapPercent;

            HotResourceSummary resourceSummary =
                    new HotResourceSummary(HEAP_MAX_SIZE, currentThreshold, previousThreshold, 0);
            nodeSummary.appendNestedSummary(resourceSummary);

            CommonStats.RCA_VERTICES_METRICS_AGGREGATOR.updateStat(
                    ADMISSION_CONTROL_RCA_TRIGGERED, instanceDetails.getInstanceId().toString(), 1);

            return new ResourceFlowUnit<>(
                    currentTimeMillis,
                    new ResourceContext(UNHEALTHY),
                    nodeSummary,
                    !instanceDetails.getIsClusterManager());
        }

        return new ResourceFlowUnit<>(
                currentTimeMillis,
                new ResourceContext(HEALTHY),
                nodeSummary,
                !instanceDetails.getIsClusterManager());
    }

    private double getThreshold(RangeConfiguration heapRange, double heapPercent) {
        Range range = heapRange.getRange(heapPercent);
        return Objects.isNull(range) ? 0 : range.getThreshold();
    }
}
