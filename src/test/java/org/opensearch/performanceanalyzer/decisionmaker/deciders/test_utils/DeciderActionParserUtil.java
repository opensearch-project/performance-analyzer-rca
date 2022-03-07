/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.test_utils;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.actions.CacheClearAction;
import org.opensearch.performanceanalyzer.decisionmaker.actions.ModifyCacheMaxSizeAction;
import org.opensearch.performanceanalyzer.decisionmaker.actions.ModifyQueueCapacityAction;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;

public class DeciderActionParserUtil {
    private final Map<ResourceEnum, ModifyCacheMaxSizeAction> cacheActionMap;
    private final Map<ResourceEnum, ModifyQueueCapacityAction> queueActionMap;
    private CacheClearAction cacheClearAction;

    public DeciderActionParserUtil() {
        cacheActionMap = new HashMap<>();
        queueActionMap = new HashMap<>();
        cacheClearAction = null;
    }

    public void addActions(List<Action> actions) throws IllegalArgumentException {
        cacheActionMap.clear();
        queueActionMap.clear();
        for (Action action : actions) {
            if (action instanceof ModifyQueueCapacityAction) {
                ModifyQueueCapacityAction queueAction = (ModifyQueueCapacityAction) action;
                assert !queueActionMap.containsKey(queueAction.getThreadPool());
                queueActionMap.put(queueAction.getThreadPool(), queueAction);
            } else if (action instanceof ModifyCacheMaxSizeAction) {
                ModifyCacheMaxSizeAction cacheAction = (ModifyCacheMaxSizeAction) action;
                assert !cacheActionMap.containsKey(cacheAction.getCacheType());
                cacheActionMap.put(cacheAction.getCacheType(), cacheAction);
            } else if (action instanceof CacheClearAction) {
                assert cacheClearAction == null;
                cacheClearAction = (CacheClearAction) action;
            } else {
                assert false;
            }
        }
    }

    public ModifyCacheMaxSizeAction readCacheAction(ResourceEnum resource) {
        return cacheActionMap.getOrDefault(resource, null);
    }

    public ModifyQueueCapacityAction readQueueAction(ResourceEnum resource) {
        return queueActionMap.getOrDefault(resource, null);
    }

    public CacheClearAction readCacheClearAction() {
        return cacheClearAction;
    }

    public int size() {
        return cacheActionMap.size() + queueActionMap.size() + (cacheClearAction == null ? 0 : 1);
    }
}
