/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.jvm.old_gen;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.DecisionPolicy;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.jvm.OldGenDecisionPolicyConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.store.rca.HighHeapUsageClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

/**
 * Decision policy for old gen related actions
 *
 * <p>This policy defines 3 levels of unhealthiness — 60-75% (level 1), 75-90% (level 2) and 90%+
 * (level 3) and create dedicated action builders {@link LevelOneActionBuilder}, {@link
 * LevelTwoActionBuilder}, {@link LevelThreeActionBuilder} for each level of unhealthiness
 */
public class OldGenDecisionPolicy implements DecisionPolicy {
    private static final Logger LOG = LogManager.getLogger(OldGenDecisionPolicy.class);
    private AppContext appContext;
    private RcaConf rcaConf;
    private final HighHeapUsageClusterRca highHeapUsageClusterRca;

    public OldGenDecisionPolicy(final HighHeapUsageClusterRca highHeapUsageClusterRca) {
        this.highHeapUsageClusterRca = highHeapUsageClusterRca;
    }

    public void setRcaConf(final RcaConf rcaConf) {
        this.rcaConf = rcaConf;
    }

    public void setAppContext(final AppContext appContext) {
        this.appContext = appContext;
    }

    @Override
    public List<Action> evaluate() {
        List<Action> actions = new ArrayList<>();
        if (highHeapUsageClusterRca.getFlowUnits().isEmpty()) {
            return actions;
        }

        ResourceFlowUnit<HotClusterSummary> flowUnit =
                highHeapUsageClusterRca.getFlowUnits().get(0);
        if (!flowUnit.hasResourceSummary()) {
            return actions;
        }
        HotClusterSummary clusterSummary = flowUnit.getSummary();
        for (HotNodeSummary nodeSummary : clusterSummary.getHotNodeSummaryList()) {
            NodeKey nodeKey = new NodeKey(nodeSummary.getNodeID(), nodeSummary.getHostAddress());
            for (HotResourceSummary resource : nodeSummary.getHotResourceSummaryList()) {
                if (resource.getResource().equals(ResourceUtil.OLD_GEN_HEAP_USAGE)) {
                    actions.addAll(evaluate(nodeKey, resource.getValue()));
                }
            }
        }
        return actions;
    }

    private List<Action> evaluate(final NodeKey nodeKey, double oldGenUsage) {
        // rca config / app context will not be null unless there is a bug in RCAScheduler.
        if (rcaConf == null || appContext == null) {
            LOG.error("rca conf/app context is null, return empty action list");
            return new ArrayList<>();
        }
        OldGenDecisionPolicyConfig oldGenDecisionPolicyConfig =
                rcaConf.getDeciderConfig().getOldGenDecisionPolicyConfig();
        if (oldGenUsage >= oldGenDecisionPolicyConfig.oldGenThresholdLevelThree()) {
            return LevelThreeActionBuilder.newBuilder(nodeKey, appContext, rcaConf).build();
        } else if (oldGenUsage >= oldGenDecisionPolicyConfig.oldGenThresholdLevelTwo()) {
            return LevelTwoActionBuilder.newBuilder(nodeKey, appContext, rcaConf).build();
        } else if (oldGenUsage >= oldGenDecisionPolicyConfig.oldGenThresholdLevelOne()) {
            return LevelOneActionBuilder.newBuilder(nodeKey, appContext, rcaConf).build();
        }
        // old gen jvm is healthy. return empty action list.
        else {
            return new ArrayList<>();
        }
    }
}
