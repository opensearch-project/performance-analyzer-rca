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
