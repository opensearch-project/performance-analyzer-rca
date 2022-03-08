/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.jvm.old_gen;


import java.util.ArrayList;
import java.util.List;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.actions.CacheClearAction;
import org.opensearch.performanceanalyzer.decisionmaker.actions.ModifyCacheMaxSizeAction;
import org.opensearch.performanceanalyzer.decisionmaker.actions.ModifyQueueCapacityAction;
import org.opensearch.performanceanalyzer.decisionmaker.actions.configs.CacheActionConfig;
import org.opensearch.performanceanalyzer.decisionmaker.actions.configs.QueueActionConfig;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.DeciderConfig;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.jvm.LevelThreeActionBuilderConfig;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.jvm.OldGenDecisionPolicyConfig;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

/**
 * build actions if old gen falls into level three bucket
 *
 * <p>if old gen usage(after full gc) falls into this bucket 90% - 100%, JVM heap is extremely
 * contended and can run into OOM at any moment. So action builder will build a group of actions to
 * scale down caches to their lower bound in one shot. And for queues we will downsize all queues
 * simultaneously with even higher steps
 */
public class LevelThreeActionBuilder {
    private final AppContext appContext;
    private final RcaConf rcaConf;
    private final NodeKey nodeKey;
    private final List<Action> actions;
    private final OldGenDecisionPolicyConfig oldGenDecisionPolicyConfig;
    private final LevelThreeActionBuilderConfig actionBuilderConfig;
    private final CacheActionConfig cacheActionConfig;
    private final QueueActionConfig queueActionConfig;

    private LevelThreeActionBuilder(
            final NodeKey nodeKey, final AppContext appContext, final RcaConf rcaConf) {
        this.appContext = appContext;
        this.rcaConf = rcaConf;
        this.nodeKey = nodeKey;
        DeciderConfig deciderConfig = rcaConf.getDeciderConfig();
        this.oldGenDecisionPolicyConfig =
                rcaConf.getDeciderConfig().getOldGenDecisionPolicyConfig();
        this.actionBuilderConfig =
                deciderConfig.getOldGenDecisionPolicyConfig().levelThreeActionBuilderConfig();
        this.cacheActionConfig = rcaConf.getCacheActionConfig();
        this.queueActionConfig = rcaConf.getQueueActionConfig();
        this.actions = new ArrayList<>();
    }

    public static LevelThreeActionBuilder newBuilder(
            final NodeKey nodeKey, final AppContext appContext, final RcaConf rcaConf) {
        return new LevelThreeActionBuilder(nodeKey, appContext, rcaConf);
    }

    // downsize field data cache to its lower bound in one shot
    public void addFieldDataCacheAction() {
        ModifyCacheMaxSizeAction action =
                ModifyCacheMaxSizeAction.newBuilder(
                                nodeKey, ResourceEnum.FIELD_DATA_CACHE, appContext, rcaConf)
                        .increase(false)
                        .setDesiredCacheMaxSizeToMin()
                        .build();
        if (action.isActionable()) {
            actions.add(action);
        }
    }

    // downsize shard request cache to its lower bound in one shot
    public void addShardRequestCacheAction() {
        ModifyCacheMaxSizeAction action =
                ModifyCacheMaxSizeAction.newBuilder(
                                nodeKey, ResourceEnum.SHARD_REQUEST_CACHE, appContext, rcaConf)
                        .increase(false)
                        .setDesiredCacheMaxSizeToMin()
                        .build();
        if (action.isActionable()) {
            actions.add(action);
        }
    }

    private void addWriteQueueAction() {
        int stepSize = queueActionConfig.getStepSize(ResourceEnum.WRITE_THREADPOOL);

        ModifyQueueCapacityAction action =
                ModifyQueueCapacityAction.newBuilder(
                                nodeKey, ResourceEnum.WRITE_THREADPOOL, appContext, rcaConf)
                        .increase(false)
                        .stepSize(stepSize * actionBuilderConfig.writeQueueStepSize())
                        .build();
        if (action.isActionable()) {
            actions.add(action);
        }
    }

    private void addSearchQueueAction() {
        int stepSize = queueActionConfig.getStepSize(ResourceEnum.SEARCH_THREADPOOL);

        ModifyQueueCapacityAction action =
                ModifyQueueCapacityAction.newBuilder(
                                nodeKey, ResourceEnum.SEARCH_THREADPOOL, appContext, rcaConf)
                        .increase(false)
                        .stepSize(stepSize * actionBuilderConfig.searchQueueStepSize())
                        .build();
        if (action.isActionable()) {
            actions.add(action);
        }
    }

    private void addCacheClearAction() {
        CacheClearAction action = CacheClearAction.newBuilder(appContext).build();
        if (action.isActionable()) {
            actions.add(action);
        }
    }

    /**
     * build actions.
     *
     * @return List of actions
     */
    public List<Action> build() {
        addFieldDataCacheAction();
        addShardRequestCacheAction();
        addSearchQueueAction();
        addWriteQueueAction();
        addCacheClearAction();
        return actions;
    }
}
