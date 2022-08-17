/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
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
        // All deciders run on the cluster_manager node, (in initial versions), so we don't expect
        // Decisions
        // to be passed over wire.
        throw new IllegalStateException(
                this.getClass().getSimpleName() + " not expected to be passed " + "over the wire.");
    }
}
