/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
