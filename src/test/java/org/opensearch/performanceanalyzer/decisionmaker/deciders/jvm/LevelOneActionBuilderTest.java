/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.jvm;

import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.actions.ModifyCacheMaxSizeAction;
import org.opensearch.performanceanalyzer.decisionmaker.actions.configs.CacheActionConfig;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.jvm.LevelOneActionBuilderConfig;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.jvm.old_gen.LevelOneActionBuilder;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.test_utils.DeciderActionParserUtil;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.store.collector.NodeConfigCache;
import org.opensearch.performanceanalyzer.rca.store.rca.cache.CacheUtil;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

public class LevelOneActionBuilderTest {

    private AppContext testAppContext;
    private NodeConfigCache dummyCache;
    private RcaConf rcaConf;
    private NodeKey node;
    private final long heapMaxSizeInBytes = 10 * CacheUtil.GB_TO_BYTES;
    private double fielddataCahceStepSize;
    private double shardRequestCacheStepSize;
    private DeciderActionParserUtil deciderActionParser;

    public LevelOneActionBuilderTest() {
        testAppContext = new AppContext();
        dummyCache = testAppContext.getNodeConfigCache();
        rcaConf = new RcaConf();
        node = new NodeKey(new InstanceDetails.Id("node-1"), new InstanceDetails.Ip("127.0.0.1"));
        deciderActionParser = new DeciderActionParserUtil();
    }

    @Before
    public void init() throws Exception {
        final String configStr =
                "{"
                        + "\"decider-config-settings\": { "
                        + "\"old-gen-decision-policy-config\": { "
                        + "\"queue-bucket-size\": 10 "
                        + "} "
                        + "} "
                        + "} ";
        rcaConf.readConfigFromString(configStr);
        dummyCache.put(node, ResourceUtil.HEAP_MAX_SIZE, heapMaxSizeInBytes);
        fielddataCahceStepSize =
                rcaConf.getCacheActionConfig().getStepSize(ResourceEnum.FIELD_DATA_CACHE);
        shardRequestCacheStepSize =
                rcaConf.getCacheActionConfig().getStepSize(ResourceEnum.SHARD_REQUEST_CACHE);
        fielddataCahceStepSize =
                fielddataCahceStepSize
                        * LevelOneActionBuilderConfig.DEFAULT_FIELD_DATA_CACHE_STEP_SIZE;
        shardRequestCacheStepSize =
                shardRequestCacheStepSize
                        * LevelOneActionBuilderConfig.DEFAULT_FIELD_DATA_CACHE_STEP_SIZE;
    }

    @Test
    public void testDownSizeAllCaches() {
        final double fielddataCacheSizeInPercent = 0.3;
        final double shardRequestCacheSizeInPercent = 0.04;
        dummyCache.put(
                node,
                ResourceUtil.FIELD_DATA_CACHE_MAX_SIZE,
                (long) (heapMaxSizeInBytes * fielddataCacheSizeInPercent));
        dummyCache.put(
                node,
                ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE,
                (long) (heapMaxSizeInBytes * shardRequestCacheSizeInPercent));
        List<Action> actions =
                LevelOneActionBuilder.newBuilder(node, testAppContext, rcaConf).build();
        deciderActionParser.addActions(actions);

        Assert.assertEquals(2, actions.size());
        long expectedCacheSize;
        long currentCacheSize;
        ModifyCacheMaxSizeAction fielddataCacheAction =
                deciderActionParser.readCacheAction(ResourceEnum.FIELD_DATA_CACHE);
        Assert.assertNotNull(fielddataCacheAction);
        expectedCacheSize =
                (long)
                        ((fielddataCacheSizeInPercent - fielddataCahceStepSize)
                                * heapMaxSizeInBytes);
        currentCacheSize = (long) (fielddataCacheSizeInPercent * heapMaxSizeInBytes);
        Assert.assertEquals(
                expectedCacheSize, fielddataCacheAction.getDesiredCacheMaxSizeInBytes(), 10);
        Assert.assertEquals(
                currentCacheSize, fielddataCacheAction.getCurrentCacheMaxSizeInBytes(), 10);

        ModifyCacheMaxSizeAction requestCacheAction =
                deciderActionParser.readCacheAction(ResourceEnum.SHARD_REQUEST_CACHE);
        Assert.assertNotNull(requestCacheAction);
        expectedCacheSize =
                (long)
                        ((shardRequestCacheSizeInPercent - shardRequestCacheStepSize)
                                * heapMaxSizeInBytes);
        currentCacheSize = (long) (shardRequestCacheSizeInPercent * heapMaxSizeInBytes);
        Assert.assertEquals(
                expectedCacheSize, requestCacheAction.getDesiredCacheMaxSizeInBytes(), 10);
        Assert.assertEquals(
                currentCacheSize, requestCacheAction.getCurrentCacheMaxSizeInBytes(), 10);
    }

    @Test
    public void testDownSizeOneCache() {
        final double fielddataCacheSizeInPercent =
                CacheActionConfig.DEFAULT_FIELDDATA_CACHE_LOWER_BOUND;
        final double shardRequestCacheSizeInPercent = 0.012;
        dummyCache.put(
                node,
                ResourceUtil.FIELD_DATA_CACHE_MAX_SIZE,
                (long) (heapMaxSizeInBytes * fielddataCacheSizeInPercent));
        dummyCache.put(
                node,
                ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE,
                (long) (heapMaxSizeInBytes * shardRequestCacheSizeInPercent));
        List<Action> actions =
                LevelOneActionBuilder.newBuilder(node, testAppContext, rcaConf).build();
        Assert.assertEquals(1, actions.size());
        Assert.assertTrue(actions.get(0).isActionable());
        Assert.assertTrue(actions.get(0) instanceof ModifyCacheMaxSizeAction);
        ModifyCacheMaxSizeAction requestCacheAction = (ModifyCacheMaxSizeAction) actions.get(0);
        Assert.assertEquals(ResourceEnum.SHARD_REQUEST_CACHE, requestCacheAction.getCacheType());
        long expectedSize =
                (long)
                        (CacheActionConfig.DEFAULT_SHARD_REQUEST_CACHE_LOWER_BOUND
                                * heapMaxSizeInBytes);
        long currSize = (long) (shardRequestCacheSizeInPercent * heapMaxSizeInBytes);
        Assert.assertEquals(expectedSize, requestCacheAction.getDesiredCacheMaxSizeInBytes(), 10);
        Assert.assertEquals(currSize, requestCacheAction.getCurrentCacheMaxSizeInBytes(), 10);
    }

    @Test
    public void testNoAvailableAction() {
        final double fielddataCacheSizeInPercent =
                CacheActionConfig.DEFAULT_FIELDDATA_CACHE_LOWER_BOUND;
        final double shardRequestCacheSizeInPercent =
                CacheActionConfig.DEFAULT_SHARD_REQUEST_CACHE_LOWER_BOUND;
        dummyCache.put(
                node,
                ResourceUtil.FIELD_DATA_CACHE_MAX_SIZE,
                (long) (heapMaxSizeInBytes * fielddataCacheSizeInPercent));
        dummyCache.put(
                node,
                ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE,
                (long) (heapMaxSizeInBytes * shardRequestCacheSizeInPercent));
        List<Action> actions =
                LevelOneActionBuilder.newBuilder(node, testAppContext, rcaConf).build();
        Assert.assertEquals(0, actions.size());
    }
}
