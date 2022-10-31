/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.admissioncontrol.heap;

import static org.opensearch.performanceanalyzer.rca.framework.api.Resources.State.HEALTHY;

import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.store.rca.admissioncontrol.model.HeapMetric;
import org.opensearch.performanceanalyzer.util.range.RangeConfiguration;

/** AdmissionControl RCA for heap > 32gb */
public class AdmissionControlByLargeHeap implements AdmissionControlByHeap {

    private InstanceDetails instanceDetails;

    @Override
    public void init(InstanceDetails instanceDetails, RangeConfiguration rangeConfiguration) {
        this.instanceDetails = instanceDetails;
    }

    @Override
    public ResourceFlowUnit<HotNodeSummary> generateFlowUnits(HeapMetric heapMetric) {
        HotNodeSummary nodeSummary =
                new HotNodeSummary(
                        instanceDetails.getInstanceId(), instanceDetails.getInstanceIp());
        return new ResourceFlowUnit<>(
                System.currentTimeMillis(),
                new ResourceContext(HEALTHY),
                nodeSummary,
                !instanceDetails.getIsMaster());
    }
}
