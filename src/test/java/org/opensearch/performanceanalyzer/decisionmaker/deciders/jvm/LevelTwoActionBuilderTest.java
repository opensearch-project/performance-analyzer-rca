/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.jvm;

import java.util.List;
import java.util.Random;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.actions.ModifyCacheMaxSizeAction;
import org.opensearch.performanceanalyzer.decisionmaker.actions.ModifyQueueCapacityAction;
import org.opensearch.performanceanalyzer.decisionmaker.actions.configs.CacheActionConfig;
import org.opensearch.performanceanalyzer.decisionmaker.actions.configs.QueueActionConfig;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.jvm.LevelTwoActionBuilderConfig;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.jvm.old_gen.LevelTwoActionBuilder;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.test_utils.DeciderActionParserUtil;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.store.collector.NodeConfigCache;
import org.opensearch.performanceanalyzer.rca.store.rca.cache.CacheUtil;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

public class LevelTwoActionBuilderTest {
    private AppContext testAppContext;
    private NodeConfigCache dummyCache;
    private RcaConf rcaConf;
    private NodeKey node;
    private final long heapMaxSizeInBytes = 10 * CacheUtil.GB_TO_BYTES;
    private int writeQueueStep;
    private int searchQueueStep;
    private double fielddataCahceStepSize;
    private double shardRequestCacheStepSize;
    private DeciderActionParserUtil deciderActionParser;

    public LevelTwoActionBuilderTest() {
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
                        + "\"queue-bucket-size\": 5 "
                        + "} "
                        + "} "
                        + "} ";
        rcaConf.readConfigFromString(configStr);
        dummyCache.put(node, ResourceUtil.HEAP_MAX_SIZE, heapMaxSizeInBytes);
        writeQueueStep = rcaConf.getQueueActionConfig().getStepSize(ResourceEnum.WRITE_THREADPOOL);
        searchQueueStep =
                rcaConf.getQueueActionConfig().getStepSize(ResourceEnum.SEARCH_THREADPOOL);
        fielddataCahceStepSize =
                rcaConf.getCacheActionConfig().getStepSize(ResourceEnum.FIELD_DATA_CACHE);
        shardRequestCacheStepSize =
                rcaConf.getCacheActionConfig().getStepSize(ResourceEnum.SHARD_REQUEST_CACHE);
        writeQueueStep = writeQueueStep * LevelTwoActionBuilderConfig.DEFAULT_WRITE_QUEUE_STEP_SIZE;
        searchQueueStep =
                searchQueueStep * LevelTwoActionBuilderConfig.DEFAULT_WRITE_QUEUE_STEP_SIZE;
        fielddataCahceStepSize =
                fielddataCahceStepSize
                        * LevelTwoActionBuilderConfig.DEFAULT_FIELD_DATA_CACHE_STEP_SIZE;
        shardRequestCacheStepSize =
                shardRequestCacheStepSize
                        * LevelTwoActionBuilderConfig.DEFAULT_FIELD_DATA_CACHE_STEP_SIZE;
    }

    @Test
    public void testDownSizeAllResources() {
        final double fielddataCacheSizeInPercent = 0.3;
        final double shardRequestCacheSizeInPercent = 0.04;
        // bucket index = 2
        final int writeQueueSize = generateQueueSize(ResourceEnum.WRITE_THREADPOOL, 6);
        // bucket index = 2
        final int searchQueueSize = generateQueueSize(ResourceEnum.SEARCH_THREADPOOL, 6);
        dummyCache.put(
                node,
                ResourceUtil.FIELD_DATA_CACHE_MAX_SIZE,
                (long) (heapMaxSizeInBytes * fielddataCacheSizeInPercent));
        dummyCache.put(
                node,
                ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE,
                (long) (heapMaxSizeInBytes * shardRequestCacheSizeInPercent));
        dummyCache.put(node, ResourceUtil.WRITE_QUEUE_CAPACITY, writeQueueSize);
        dummyCache.put(node, ResourceUtil.SEARCH_QUEUE_CAPACITY, searchQueueSize);
        List<Action> actions =
                LevelTwoActionBuilder.newBuilder(node, testAppContext, rcaConf).build();
        deciderActionParser.addActions(actions);
        Assert.assertEquals(4, deciderActionParser.size());
        int expectedQueueSize;
        ModifyQueueCapacityAction writeQueueAction =
                deciderActionParser.readQueueAction(ResourceEnum.WRITE_THREADPOOL);
        Assert.assertNotNull(writeQueueAction);
        expectedQueueSize = writeQueueSize - writeQueueStep;
        Assert.assertEquals(expectedQueueSize, writeQueueAction.getDesiredCapacity());
        Assert.assertEquals(writeQueueSize, writeQueueAction.getCurrentCapacity());

        ModifyQueueCapacityAction searchQueueAction =
                deciderActionParser.readQueueAction(ResourceEnum.SEARCH_THREADPOOL);
        Assert.assertNotNull(searchQueueAction);
        expectedQueueSize = searchQueueSize - searchQueueStep;
        Assert.assertEquals(expectedQueueSize, searchQueueAction.getDesiredCapacity());
        Assert.assertEquals(searchQueueSize, searchQueueAction.getCurrentCapacity());

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
    public void testDownSizeActionableResources() {
        final double fielddataCacheSizeInPercent = 0.3;
        final double shardRequestCacheSizeInPercent =
                CacheActionConfig.DEFAULT_SHARD_REQUEST_CACHE_LOWER_BOUND;
        final int writeQueueSize = QueueActionConfig.DEFAULT_WRITE_QUEUE_LOWER_BOUND;
        final int searchQueueSize = 2000;
        dummyCache.put(
                node,
                ResourceUtil.FIELD_DATA_CACHE_MAX_SIZE,
                (long) (heapMaxSizeInBytes * fielddataCacheSizeInPercent));
        dummyCache.put(
                node,
                ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE,
                (long) (heapMaxSizeInBytes * shardRequestCacheSizeInPercent));
        dummyCache.put(node, ResourceUtil.WRITE_QUEUE_CAPACITY, writeQueueSize);
        dummyCache.put(node, ResourceUtil.SEARCH_QUEUE_CAPACITY, searchQueueSize);
        List<Action> actions =
                LevelTwoActionBuilder.newBuilder(node, testAppContext, rcaConf).build();
        deciderActionParser.addActions(actions);

        Assert.assertEquals(2, deciderActionParser.size());

        int expectedQueueSize;
        ModifyQueueCapacityAction writeQueueAction =
                deciderActionParser.readQueueAction(ResourceEnum.WRITE_THREADPOOL);
        Assert.assertNull(writeQueueAction);

        ModifyQueueCapacityAction searchQueueAction =
                deciderActionParser.readQueueAction(ResourceEnum.SEARCH_THREADPOOL);
        Assert.assertNotNull(searchQueueAction);
        expectedQueueSize = searchQueueSize - searchQueueStep;
        Assert.assertEquals(expectedQueueSize, searchQueueAction.getDesiredCapacity());
        Assert.assertEquals(searchQueueSize, searchQueueAction.getCurrentCapacity());

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
        Assert.assertNull(requestCacheAction);
    }

    private void updateWorkLoadType(boolean preferIngest) throws Exception {
        final String configStr;
        if (preferIngest) {
            configStr =
                    "{"
                            + "\"decider-config-settings\": { "
                            + "\"workload-type\": {"
                            + "\"prefer-ingest\": true"
                            + "}, "
                            + "\"old-gen-decision-policy-config\": { "
                            + "\"queue-bucket-size\": 20 "
                            + "} "
                            + "} "
                            + "} ";
        } else {
            configStr =
                    "{"
                            + "\"decider-config-settings\": { "
                            + "\"workload-type\": {"
                            + "\"prefer-search\": true"
                            + "}, "
                            + "\"old-gen-decision-policy-config\": { "
                            + "\"queue-bucket-size\": 20 "
                            + "} "
                            + "} "
                            + "} ";
        }
        rcaConf.readConfigFromString(configStr);
    }

    // generate a random queue size within the given bucket index
    private int generateQueueSize(ResourceEnum queueType, int index) {
        Random rand = new Random();
        int queueStepSize = rcaConf.getQueueActionConfig().getStepSize(queueType);
        int lowerBound = rcaConf.getQueueActionConfig().getThresholdConfig(queueType).lowerBound();
        return lowerBound + index * queueStepSize + rand.nextInt(queueStepSize);
    }

    @Test
    public void testSameBucketsAndPreferIngest() throws Exception {
        updateWorkLoadType(true);
        final double fielddataCacheSizeInPercent =
                CacheActionConfig.DEFAULT_FIELDDATA_CACHE_LOWER_BOUND;
        final double shardRequestCacheSizeInPercent =
                CacheActionConfig.DEFAULT_SHARD_REQUEST_CACHE_LOWER_BOUND;
        int writeQueueStepSize =
                rcaConf.getQueueActionConfig().getStepSize(ResourceEnum.WRITE_THREADPOOL);
        int searchQueueStepSize =
                rcaConf.getQueueActionConfig().getStepSize(ResourceEnum.SEARCH_THREADPOOL);
        // bucket size for search queue = 100 / write queue = 40
        // bucket index = 2
        final int writeQueueSize = generateQueueSize(ResourceEnum.WRITE_THREADPOOL, 2);
        // bucket index = 2
        final int searchQueueSize = generateQueueSize(ResourceEnum.SEARCH_THREADPOOL, 2);
        dummyCache.put(
                node,
                ResourceUtil.FIELD_DATA_CACHE_MAX_SIZE,
                (long) (heapMaxSizeInBytes * fielddataCacheSizeInPercent));
        dummyCache.put(
                node,
                ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE,
                (long) (heapMaxSizeInBytes * shardRequestCacheSizeInPercent));
        dummyCache.put(node, ResourceUtil.WRITE_QUEUE_CAPACITY, writeQueueSize);
        dummyCache.put(node, ResourceUtil.SEARCH_QUEUE_CAPACITY, searchQueueSize);
        List<Action> actions =
                LevelTwoActionBuilder.newBuilder(node, testAppContext, rcaConf).build();
        deciderActionParser.addActions(actions);

        Assert.assertEquals(1, deciderActionParser.size());
        ModifyCacheMaxSizeAction fielddataCacheAction =
                deciderActionParser.readCacheAction(ResourceEnum.FIELD_DATA_CACHE);
        Assert.assertNull(fielddataCacheAction);
        ModifyCacheMaxSizeAction requestCacheAction =
                deciderActionParser.readCacheAction(ResourceEnum.SHARD_REQUEST_CACHE);
        Assert.assertNull(requestCacheAction);
        ModifyQueueCapacityAction writeQueueAction =
                deciderActionParser.readQueueAction(ResourceEnum.WRITE_THREADPOOL);
        Assert.assertNull(writeQueueAction);

        ModifyQueueCapacityAction searchQueueAction =
                deciderActionParser.readQueueAction(ResourceEnum.SEARCH_THREADPOOL);
        Assert.assertNotNull(searchQueueAction);
        Assert.assertTrue(searchQueueAction.isActionable());
        int expectedQueueSize = searchQueueSize - searchQueueStep;
        Assert.assertEquals(expectedQueueSize, searchQueueAction.getDesiredCapacity());
        Assert.assertEquals(searchQueueSize, searchQueueAction.getCurrentCapacity());
    }

    @Test
    public void testSameBucketsAndPreferSearch() throws Exception {
        updateWorkLoadType(false);
        final double fielddataCacheSizeInPercent =
                CacheActionConfig.DEFAULT_FIELDDATA_CACHE_LOWER_BOUND;
        final double shardRequestCacheSizeInPercent =
                CacheActionConfig.DEFAULT_SHARD_REQUEST_CACHE_LOWER_BOUND;
        // bucket index = 0
        final int writeQueueSize = generateQueueSize(ResourceEnum.WRITE_THREADPOOL, 0);
        // bucket index = 0
        final int searchQueueSize = generateQueueSize(ResourceEnum.SEARCH_THREADPOOL, 0);
        dummyCache.put(
                node,
                ResourceUtil.FIELD_DATA_CACHE_MAX_SIZE,
                (long) (heapMaxSizeInBytes * fielddataCacheSizeInPercent));
        dummyCache.put(
                node,
                ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE,
                (long) (heapMaxSizeInBytes * shardRequestCacheSizeInPercent));
        dummyCache.put(node, ResourceUtil.WRITE_QUEUE_CAPACITY, writeQueueSize);
        dummyCache.put(node, ResourceUtil.SEARCH_QUEUE_CAPACITY, searchQueueSize);
        List<Action> actions =
                LevelTwoActionBuilder.newBuilder(node, testAppContext, rcaConf).build();
        deciderActionParser.addActions(actions);

        Assert.assertEquals(1, deciderActionParser.size());
        ModifyCacheMaxSizeAction fielddataCacheAction =
                deciderActionParser.readCacheAction(ResourceEnum.FIELD_DATA_CACHE);
        Assert.assertNull(fielddataCacheAction);
        ModifyCacheMaxSizeAction requestCacheAction =
                deciderActionParser.readCacheAction(ResourceEnum.SHARD_REQUEST_CACHE);
        Assert.assertNull(requestCacheAction);
        ModifyQueueCapacityAction writeQueueAction =
                deciderActionParser.readQueueAction(ResourceEnum.WRITE_THREADPOOL);
        Assert.assertNotNull(writeQueueAction);
        ModifyQueueCapacityAction searchQueueAction =
                deciderActionParser.readQueueAction(ResourceEnum.SEARCH_THREADPOOL);
        Assert.assertNull(searchQueueAction);
        Assert.assertTrue(writeQueueAction.isActionable());
        Assert.assertEquals(
                QueueActionConfig.DEFAULT_WRITE_QUEUE_LOWER_BOUND,
                writeQueueAction.getDesiredCapacity());
        Assert.assertEquals(writeQueueSize, writeQueueAction.getCurrentCapacity());
    }

    @Test
    public void testSearchQueueHasLargerBucketIndex() throws Exception {
        updateWorkLoadType(false);
        final double fielddataCacheSizeInPercent =
                CacheActionConfig.DEFAULT_FIELDDATA_CACHE_LOWER_BOUND;
        final double shardRequestCacheSizeInPercent =
                CacheActionConfig.DEFAULT_SHARD_REQUEST_CACHE_LOWER_BOUND;
        // bucket index = 1
        final int writeQueueSize = generateQueueSize(ResourceEnum.WRITE_THREADPOOL, 1);
        // bucket index = 5
        final int searchQueueSize = generateQueueSize(ResourceEnum.SEARCH_THREADPOOL, 5);
        dummyCache.put(
                node,
                ResourceUtil.FIELD_DATA_CACHE_MAX_SIZE,
                (long) (heapMaxSizeInBytes * fielddataCacheSizeInPercent));
        dummyCache.put(
                node,
                ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE,
                (long) (heapMaxSizeInBytes * shardRequestCacheSizeInPercent));
        dummyCache.put(node, ResourceUtil.WRITE_QUEUE_CAPACITY, writeQueueSize);
        dummyCache.put(node, ResourceUtil.SEARCH_QUEUE_CAPACITY, searchQueueSize);
        List<Action> actions =
                LevelTwoActionBuilder.newBuilder(node, testAppContext, rcaConf).build();
        deciderActionParser.addActions(actions);

        Assert.assertEquals(1, deciderActionParser.size());
        ModifyCacheMaxSizeAction fielddataCacheAction =
                deciderActionParser.readCacheAction(ResourceEnum.FIELD_DATA_CACHE);
        Assert.assertNull(fielddataCacheAction);
        ModifyCacheMaxSizeAction requestCacheAction =
                deciderActionParser.readCacheAction(ResourceEnum.SHARD_REQUEST_CACHE);
        Assert.assertNull(requestCacheAction);
        ModifyQueueCapacityAction writeQueueAction =
                deciderActionParser.readQueueAction(ResourceEnum.WRITE_THREADPOOL);
        Assert.assertNull(writeQueueAction);

        ModifyQueueCapacityAction searchQueueAction =
                deciderActionParser.readQueueAction(ResourceEnum.SEARCH_THREADPOOL);
        Assert.assertNotNull(searchQueueAction);
        Assert.assertTrue(searchQueueAction.isActionable());
        int expectedQueueSize = searchQueueSize - searchQueueStep;
        Assert.assertEquals(expectedQueueSize, searchQueueAction.getDesiredCapacity());
        Assert.assertEquals(searchQueueSize, searchQueueAction.getCurrentCapacity());
    }

    @Test
    public void testWriteQueueHasLargerBucketIndex() throws Exception {
        updateWorkLoadType(true);
        final double fielddataCacheSizeInPercent =
                CacheActionConfig.DEFAULT_FIELDDATA_CACHE_LOWER_BOUND;
        final double shardRequestCacheSizeInPercent =
                CacheActionConfig.DEFAULT_SHARD_REQUEST_CACHE_LOWER_BOUND;
        // bucket index = 4
        final int writeQueueSize = generateQueueSize(ResourceEnum.WRITE_THREADPOOL, 4);
        // bucket index = 2
        final int searchQueueSize = generateQueueSize(ResourceEnum.SEARCH_THREADPOOL, 2);
        dummyCache.put(
                node,
                ResourceUtil.FIELD_DATA_CACHE_MAX_SIZE,
                (long) (heapMaxSizeInBytes * fielddataCacheSizeInPercent));
        dummyCache.put(
                node,
                ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE,
                (long) (heapMaxSizeInBytes * shardRequestCacheSizeInPercent));
        dummyCache.put(node, ResourceUtil.WRITE_QUEUE_CAPACITY, writeQueueSize);
        dummyCache.put(node, ResourceUtil.SEARCH_QUEUE_CAPACITY, searchQueueSize);
        List<Action> actions =
                LevelTwoActionBuilder.newBuilder(node, testAppContext, rcaConf).build();
        deciderActionParser.addActions(actions);

        Assert.assertEquals(1, deciderActionParser.size());
        ModifyCacheMaxSizeAction fielddataCacheAction =
                deciderActionParser.readCacheAction(ResourceEnum.FIELD_DATA_CACHE);
        Assert.assertNull(fielddataCacheAction);
        ModifyCacheMaxSizeAction requestCacheAction =
                deciderActionParser.readCacheAction(ResourceEnum.SHARD_REQUEST_CACHE);
        Assert.assertNull(requestCacheAction);
        ModifyQueueCapacityAction writeQueueAction =
                deciderActionParser.readQueueAction(ResourceEnum.WRITE_THREADPOOL);
        Assert.assertNotNull(writeQueueAction);
        ModifyQueueCapacityAction searchQueueAction =
                deciderActionParser.readQueueAction(ResourceEnum.SEARCH_THREADPOOL);
        Assert.assertNull(searchQueueAction);
        Assert.assertTrue(writeQueueAction.isActionable());
        int expectedQueueSize = writeQueueSize - writeQueueStep;
        Assert.assertEquals(expectedQueueSize, writeQueueAction.getDesiredCapacity());
        Assert.assertEquals(writeQueueSize, writeQueueAction.getCurrentCapacity());
    }
}
