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
 * Copyright 2020-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
