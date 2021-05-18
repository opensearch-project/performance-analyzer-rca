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
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.temperature.CompactNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;

/**
 * This is a grpc wrapper on top of the CompactNodeTemperatureSummary. The flow unit is passed
 * around from the data nodes to the master. Its compact, because it does not contain the
 * temperatures at the granularity of shards. As, some of our largest instances can have multiple
 * shards, it would be sending too many bytes over the wire.
 */
public class CompactNodeTemperatureFlowUnit extends ResourceFlowUnit<CompactNodeSummary> {
    private final CompactNodeSummary compactNodeTemperatureSummary;

    public CompactNodeTemperatureFlowUnit(
            long timeStamp,
            ResourceContext context,
            CompactNodeSummary resourceSummary,
            boolean persistSummary) {
        super(timeStamp, context, resourceSummary, persistSummary);
        this.compactNodeTemperatureSummary = resourceSummary;
    }

    public CompactNodeTemperatureFlowUnit(long timeStamp) {
        super(timeStamp);
        compactNodeTemperatureSummary = null;
    }

    @Override
    public FlowUnitMessage buildFlowUnitMessage(String graphNode, InstanceDetails.Id node) {
        FlowUnitMessage.Builder builder = FlowUnitMessage.newBuilder();
        builder.setGraphNode(graphNode);
        builder.setNode(node.toString());
        builder.setTimeStamp(System.currentTimeMillis());
        if (compactNodeTemperatureSummary != null) {
            compactNodeTemperatureSummary.buildSummaryMessageAndAddToFlowUnit(builder);
        }
        return builder.build();
    }

    public static CompactNodeTemperatureFlowUnit buildFlowUnitFromWrapper(
            final FlowUnitMessage message) {
        CompactNodeSummary compactNodeTemperatureSummary =
                CompactNodeSummary.buildNodeTemperatureProfileFromMessage(
                        message.getNodeTemperatureSummary());
        return new CompactNodeTemperatureFlowUnit(
                message.getTimeStamp(),
                new ResourceContext(Resources.State.UNKNOWN),
                compactNodeTemperatureSummary,
                false);
    }

    public CompactNodeSummary getCompactNodeTemperatureSummary() {
        return compactNodeTemperatureSummary;
    }
}
