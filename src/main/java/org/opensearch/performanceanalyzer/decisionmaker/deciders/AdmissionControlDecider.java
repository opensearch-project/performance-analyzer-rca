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

package org.opensearch.performanceanalyzer.decisionmaker.deciders;

import static org.opensearch.performanceanalyzer.rca.store.rca.admissioncontrol.AdmissionControlRca.REQUEST_SIZE;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.opensearch.performanceanalyzer.decisionmaker.actions.AdmissionControlAction;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.store.rca.admissioncontrol.AdmissionControlClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

public class AdmissionControlDecider extends Decider {

    private int counter = 0;
    private static final String NAME = "admissionControlDecider";
    private final AdmissionControlClusterRca admissionControlClusterRca;

    public AdmissionControlDecider(
            long evalIntervalSeconds,
            int decisionFrequency,
            AdmissionControlClusterRca admissionControlClusterRca) {
        super(evalIntervalSeconds, decisionFrequency);
        this.admissionControlClusterRca = admissionControlClusterRca;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Decision operate() {
        Decision decision = new Decision(Instant.now().toEpochMilli(), NAME);
        counter += 1;
        if (counter < decisionFrequency) {
            return decision;
        }
        counter = 0;

        List<AdmissionControlAction> heapBasedActions = getHeapBasedActions();
        heapBasedActions.stream()
                .max(Comparator.comparingDouble(AdmissionControlAction::getDesiredValue))
                .ifPresent(decision::addAction);

        return decision;
    }

    private List<AdmissionControlAction> getHeapBasedActions() {
        List<AdmissionControlAction> heapBasedActions = new ArrayList<>();

        if (admissionControlClusterRca.getFlowUnits().isEmpty()) {
            return heapBasedActions;
        }

        ResourceFlowUnit<HotClusterSummary> flowUnit =
                admissionControlClusterRca.getFlowUnits().get(0);
        if (!flowUnit.hasResourceSummary()) {
            return heapBasedActions;
        }

        HotClusterSummary clusterSummary = flowUnit.getSummary();
        clusterSummary
                .getHotNodeSummaryList()
                .forEach(
                        nodeSummary -> {
                            nodeSummary.getHotResourceSummaryList().stream()
                                    .filter(this::isHeapResource)
                                    .map(resourceSummary -> getAction(nodeSummary, resourceSummary))
                                    .filter(Objects::nonNull)
                                    .forEach(heapBasedActions::add);
                        });

        return heapBasedActions;
    }

    private boolean isHeapResource(HotResourceSummary hotResourceSummary) {
        return hotResourceSummary.getResource().getResourceEnum() == ResourceEnum.HEAP;
    }

    private AdmissionControlAction getAction(
            HotNodeSummary hotNodeSummary, HotResourceSummary hotResourceSummary) {
        double currentThreshold = hotResourceSummary.getValue();
        double desiredThreshold = hotResourceSummary.getThreshold();
        NodeKey esNode = new NodeKey(hotNodeSummary.getNodeID(), hotNodeSummary.getHostAddress());
        AdmissionControlAction action =
                AdmissionControlAction.newBuilder(esNode, REQUEST_SIZE, getAppContext(), rcaConf)
                        .currentValue(currentThreshold)
                        .desiredValue(desiredThreshold)
                        .build();
        return action.isActionable() ? action : null;
    }
}
