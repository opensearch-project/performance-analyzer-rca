/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.admissioncontrol.heap;

import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.store.rca.admissioncontrol.model.HeapMetric;
import org.opensearch.performanceanalyzer.util.range.RangeConfiguration;

/** Interface that can be implemented to calculate ResourceFlowUnit for various heap size */
public interface AdmissionControlByHeap {
    void init(InstanceDetails instanceDetails, RangeConfiguration rangeConfiguration);

    ResourceFlowUnit<HotNodeSummary> generateFlowUnits(HeapMetric heapMetric);
}
