/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders;


import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.actions.ModifyQueueCapacityAction;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaRuntimeMetrics;
import org.opensearch.performanceanalyzer.rca.store.rca.HighHeapUsageClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.QueueRejectionClusterRca;

// This is a sample decider implementation to finalize decision maker interfaces.
// TODO: 1. Read action priorities from a configurable yml

public class QueueHealthDecider extends HeapBasedDecider {

    private static final Logger LOG = LogManager.getLogger(Decider.class);
    public static final String NAME = "queue_health";

    private QueueRejectionClusterRca queueRejectionRca;
    List<String> actionsByUserPriority = new ArrayList<>();
    private int counter = 0;

    public QueueHealthDecider(
            long evalIntervalSeconds,
            int decisionFrequency,
            QueueRejectionClusterRca queueRejectionClusterRca,
            HighHeapUsageClusterRca highHeapUsageClusterRca) {
        super(evalIntervalSeconds, decisionFrequency, highHeapUsageClusterRca);
        this.queueRejectionRca = queueRejectionClusterRca;
        configureActionPriority();
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Decision operate() {
        Decision decision = new Decision(System.currentTimeMillis(), NAME);
        counter += 1;
        if (counter < decisionFrequency) {
            return decision;
        }

        counter = 0;
        if (queueRejectionRca.getFlowUnits().isEmpty()) {
            return decision;
        }

        ResourceFlowUnit<HotClusterSummary> flowUnit = queueRejectionRca.getFlowUnits().get(0);
        if (!flowUnit.hasResourceSummary()) {
            return decision;
        }
        HotClusterSummary clusterSummary = flowUnit.getSummary();
        for (HotNodeSummary nodeSummary : clusterSummary.getHotNodeSummaryList()) {
            NodeKey nodeKey = new NodeKey(nodeSummary.getNodeID(), nodeSummary.getHostAddress());
            for (HotResourceSummary resource : nodeSummary.getHotResourceSummaryList()) {
                decision.addAction(
                        computeBestAction(nodeKey, resource.getResource().getResourceEnum()));
            }
        }
        return decision;
    }

    private void configureActionPriority() {
        // TODO: Input from user configured yml
        this.actionsByUserPriority.add(ModifyQueueCapacityAction.NAME);
    }

    /**
     * Evaluate the most relevant action for a node
     *
     * <p>Action relevance decided based on user configured priorities for now, this can be modified
     * to consume better signals going forward.
     */
    private Action computeBestAction(NodeKey nodeKey, ResourceEnum threadPool) {
        Action action = null;
        if (canUseMoreHeap(nodeKey)) {
            for (String actionName : actionsByUserPriority) {
                action = getAction(actionName, nodeKey, threadPool, true);
                if (action != null) {
                    break;
                }
            }
        } else {
            PerformanceAnalyzerApp.RCA_RUNTIME_METRICS_AGGREGATOR.updateStat(
                    RcaRuntimeMetrics.NO_INCREASE_ACTION_SUGGESTED,
                    NAME + ":" + nodeKey.getHostAddress(),
                    1);
        }
        return action;
    }

    private Action getAction(
            String actionName, NodeKey nodeKey, ResourceEnum threadPool, boolean increase) {
        switch (actionName) {
            case ModifyQueueCapacityAction.NAME:
                return configureQueueCapacity(nodeKey, threadPool, increase);
            default:
                return null;
        }
    }

    private ModifyQueueCapacityAction configureQueueCapacity(
            NodeKey nodeKey, ResourceEnum threadPool, boolean increase) {
        ModifyQueueCapacityAction action =
                ModifyQueueCapacityAction.newBuilder(nodeKey, threadPool, getAppContext(), rcaConf)
                        .increase(increase)
                        .build();
        if (action != null && action.isActionable()) {
            return action;
        }
        return null;
    }
}
