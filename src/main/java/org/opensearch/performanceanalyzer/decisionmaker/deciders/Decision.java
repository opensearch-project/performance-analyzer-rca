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


import java.util.ArrayList;
import java.util.List;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;

public class Decision extends GenericFlowUnit {

    private List<Action> actions = new ArrayList<>();
    private String decider;

    public Decision(long timeStamp, String deciderName) {
        super(timeStamp);
        setDecider(deciderName);
    }

    public void addAction(Action action) {
        if (action != null) {
            actions.add(action);
        }
    }

    public void addAllActions(List<Action> actions) {
        this.actions.addAll(actions);
    }

    public List<Action> getActions() {
        return actions;
    }

    public String getDecider() {
        return decider;
    }

    public void setDecider(String decider) {
        this.decider = decider;
    }

    @Override
    public String toString() {
        return decider + " : " + actions;
    }

    @Override
    public boolean isEmpty() {
        return actions.isEmpty();
    }

    @Override
    public FlowUnitMessage buildFlowUnitMessage(String graphNode, InstanceDetails.Id node) {
        // All deciders run on the master node, (in initial versions), so we dont expect Decisions
        // to be passed over wire.
        throw new IllegalStateException(
                this.getClass().getSimpleName() + " not expected to be passed " + "over the wire.");
    }
}
