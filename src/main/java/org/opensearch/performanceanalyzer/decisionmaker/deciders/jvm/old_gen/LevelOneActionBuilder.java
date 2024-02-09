/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.jvm.old_gen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.actions.ModifyCacheMaxSizeAction;
import org.opensearch.performanceanalyzer.decisionmaker.actions.configs.CacheActionConfig;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.jvm.LevelOneActionBuilderConfig;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.jvm.OldGenDecisionPolicyConfig;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

/**
 * build actions if old gen falls into level one bucket
 *
 * <p>if old gen usage(after full gc) is between 60% - 75%, it has not yet reach the 75% threshold
 * in GC. So we will downsize each cache by regular step size and leave the queues untouched. By
 * default Actions in this bucket will ignore priority settings of caches and downsize both caches
 * simultaneously until the capacity of those caches reaches the lower bounds.
 *
 * <p>For field data cache, the lower bound in this bucket is 10% of the heap and for shard request
 * cache / query cache, it will be 2% of the heap(default OpenSearch settings). This is to free up
 * excessive heap used by fielddata cache or query cache because JVM decider favors stability over
 * performance.
 */
public class LevelOneActionBuilder {
    private final AppContext appContext;
    private final RcaConf rcaConf;
    private final OldGenDecisionPolicyConfig oldGenDecisionPolicyConfig;
    private final LevelOneActionBuilderConfig actionBuilderConfig;
    private final CacheActionConfig cacheActionConfig;
    private final NodeKey nodeKey;
    private final Map<ResourceEnum, ModifyCacheMaxSizeAction> cacheActionMap;

    private LevelOneActionBuilder(
            final NodeKey nodeKey, final AppContext appContext, final RcaConf rcaConf) {
        this.appContext = appContext;
        this.rcaConf = rcaConf;
        this.oldGenDecisionPolicyConfig =
                rcaConf.getDeciderConfig().getOldGenDecisionPolicyConfig();
        this.actionBuilderConfig = this.oldGenDecisionPolicyConfig.levelOneActionBuilderConfig();
        this.cacheActionConfig = rcaConf.getCacheActionConfig();
        this.nodeKey = nodeKey;
        this.cacheActionMap = new HashMap<>();
        registerActions();
    }

    public static LevelOneActionBuilder newBuilder(
            final NodeKey nodeKey, final AppContext appContext, final RcaConf rcaConf) {
        return new LevelOneActionBuilder(nodeKey, appContext, rcaConf);
    }

    private void addFieldDataCacheAction() {
        double stepSizeInPercent = cacheActionConfig.getStepSize(ResourceEnum.FIELD_DATA_CACHE);

        ModifyCacheMaxSizeAction action =
                ModifyCacheMaxSizeAction.newBuilder(
                                nodeKey, ResourceEnum.FIELD_DATA_CACHE, appContext, rcaConf)
                        .increase(false)
                        .stepSizeInPercent(
                                stepSizeInPercent * actionBuilderConfig.fieldDataCacheStepSize())
                        .build();
        if (action.isActionable()) {
            cacheActionMap.put(ResourceEnum.FIELD_DATA_CACHE, action);
        }
    }

    private void addShardRequestCacheAction() {
        double stepSizeInPercent = cacheActionConfig.getStepSize(ResourceEnum.SHARD_REQUEST_CACHE);

        ModifyCacheMaxSizeAction action =
                ModifyCacheMaxSizeAction.newBuilder(
                                nodeKey, ResourceEnum.SHARD_REQUEST_CACHE, appContext, rcaConf)
                        .increase(false)
                        .stepSizeInPercent(
                                stepSizeInPercent * actionBuilderConfig.shardRequestCacheStepSize())
                        .build();
        if (action.isActionable()) {
            cacheActionMap.put(ResourceEnum.SHARD_REQUEST_CACHE, action);
        }
    }

    private void registerActions() {
        addFieldDataCacheAction();
        addShardRequestCacheAction();
    }

    /**
     * build actions.
     *
     * @return List of actions
     */
    public List<Action> build() {
        List<Action> actions = new ArrayList<>();
        cacheActionMap.forEach(
                (cache, action) -> {
                    actions.add(action);
                });
        return actions;
    }
}
