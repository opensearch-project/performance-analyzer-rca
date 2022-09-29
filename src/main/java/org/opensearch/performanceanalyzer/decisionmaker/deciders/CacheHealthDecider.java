/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders;


import com.google.common.collect.ImmutableMap;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.actions.ModifyCacheMaxSizeAction;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaRuntimeMetrics;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.store.rca.HighHeapUsageClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.BaseClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.FieldDataCacheClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.ShardRequestCacheClusterRca;

// TODO: 1. Create separate ActionConfig objects for different actions

public class CacheHealthDecider extends HeapBasedDecider {
    private static final Logger LOG = LogManager.getLogger(CacheHealthDecider.class);
    public static final String NAME = "cacheHealthDecider";

    private final FieldDataCacheClusterRca fieldDataCacheClusterRca;
    private final ShardRequestCacheClusterRca shardRequestCacheClusterRca;
    private final ImmutableMap<ResourceEnum, BaseClusterRca> cacheTypeBaseClusterRcaMap;

    List<ResourceEnum> modifyCacheActionPriorityList = new ArrayList<>();
    private int counter = 0;

    public CacheHealthDecider(
            final long evalIntervalSeconds,
            final int decisionFrequency,
            final FieldDataCacheClusterRca fieldDataCacheClusterRca,
            final ShardRequestCacheClusterRca shardRequestCacheClusterRca,
            final HighHeapUsageClusterRca highHeapUsageClusterRca) {
        super(evalIntervalSeconds, decisionFrequency, highHeapUsageClusterRca);
        configureModifyCacheActionPriority();

        this.fieldDataCacheClusterRca = fieldDataCacheClusterRca;
        this.shardRequestCacheClusterRca = shardRequestCacheClusterRca;
        this.cacheTypeBaseClusterRcaMap =
                ImmutableMap.<ResourceEnum, BaseClusterRca>builder()
                        .put(ResourceEnum.SHARD_REQUEST_CACHE, shardRequestCacheClusterRca)
                        .put(ResourceEnum.FIELD_DATA_CACHE, fieldDataCacheClusterRca)
                        .build();
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Decision operate() {
        final Set<InstanceDetails.Id> impactedNodes = new HashSet<>();

        Decision decision = new Decision(System.currentTimeMillis(), NAME);
        counter += 1;
        if (counter < decisionFrequency) {
            return decision;
        }
        counter = 0;

        for (final ResourceEnum cacheType : modifyCacheActionPriorityList) {
            BaseClusterRca baseClusterRcaMap = cacheTypeBaseClusterRcaMap.get(cacheType);
            if (baseClusterRcaMap == null)
                continue;
            getActionsFromRca(baseClusterRcaMap, impactedNodes).forEach(decision::addAction);
        }
        return decision;
    }

    private <R extends BaseClusterRca> List<Action> getActionsFromRca(
            final R cacheClusterRca, final Set<InstanceDetails.Id> impactedNodes) {
        final List<Action> actions = new ArrayList<>();

        if (!cacheClusterRca.getFlowUnits().isEmpty()) {
            final ResourceFlowUnit<HotClusterSummary> flowUnit =
                    cacheClusterRca.getFlowUnits().get(0);
            if (!flowUnit.hasResourceSummary()) {
                return actions;
            }

            final List<HotNodeSummary> clusterSummary =
                    flowUnit.getSummary().getHotNodeSummaryList();

            for (final HotNodeSummary hotNodeSummary : clusterSummary) {
                if (!impactedNodes.contains(hotNodeSummary.getNodeID())) {
                    final NodeKey nodeKey =
                            new NodeKey(
                                    hotNodeSummary.getNodeID(), hotNodeSummary.getHostAddress());
                    for (final HotResourceSummary resource :
                            hotNodeSummary.getHotResourceSummaryList()) {
                        final Action action =
                                computeBestAction(
                                        nodeKey, resource.getResource().getResourceEnum());
                        if (action != null) {
                            actions.add(action);
                            impactedNodes.add(hotNodeSummary.getNodeID());
                        }
                    }
                }
            }
        }
        return actions;
    }

    private void configureModifyCacheActionPriority() {
        this.modifyCacheActionPriorityList.add(ResourceEnum.SHARD_REQUEST_CACHE);
        this.modifyCacheActionPriorityList.add(ResourceEnum.FIELD_DATA_CACHE);
    }

    /**
     * Evaluate the most relevant action for a node for the specific cache type
     *
     * <p>Only ModifyCacheMaxSize Action is used for now, this can be modified to consume better
     * signals going forward.
     */
    private Action computeBestAction(final NodeKey nodeKey, final ResourceEnum cacheType) {
        Action action = null;
        if (canUseMoreHeap(nodeKey)) {
            action = getAction(ModifyCacheMaxSizeAction.NAME, nodeKey, cacheType, true);
        } else {
            PerformanceAnalyzerApp.RCA_RUNTIME_METRICS_AGGREGATOR.updateStat(
                    RcaRuntimeMetrics.NO_INCREASE_ACTION_SUGGESTED,
                    NAME + ":" + nodeKey.getHostAddress(),
                    1);
        }
        return action;
    }

    private Action getAction(
            final String actionName,
            final NodeKey nodeKey,
            final ResourceEnum cacheType,
            final boolean increase) {
        if (ModifyCacheMaxSizeAction.NAME.equals(actionName)) {
            return configureCacheMaxSize(nodeKey, cacheType, increase);
        }
        return null;
    }

    private ModifyCacheMaxSizeAction configureCacheMaxSize(
            final NodeKey nodeKey, final ResourceEnum cacheType, final boolean increase) {
        final ModifyCacheMaxSizeAction action =
                ModifyCacheMaxSizeAction.newBuilder(nodeKey, cacheType, getAppContext(), rcaConf)
                        .increase(increase)
                        .build();
        if (action.isActionable()) {
            return action;
        }
        return null;
    }
}
