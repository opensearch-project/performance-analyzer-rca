/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.decisionmaker.actions.configs.CacheActionConfig;
import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;
import org.opensearch.performanceanalyzer.rca.store.rca.cache.CacheUtil;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

public class ModifyCacheMaxSizeActionTest {
    private final long heapMaxSizeInBytes = 32 * CacheUtil.GB_TO_BYTES;
    private final long fieldDataCacheMaxSizeInBytes = 10 * CacheUtil.GB_TO_BYTES;
    private final long shardRequestCacheMaxSizeInBytes = 1000 * CacheUtil.MB_TO_BYTES;

    private AppContext appContext;
    private RcaConf rcaConf;

    public ModifyCacheMaxSizeActionTest() {
        String rcaConfPath = Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca.conf").toString();
        rcaConf = new RcaConf(rcaConfPath);
    }

    @Before
    public void setUp() {
        appContext = new AppContext();
    }

    @Test
    public void testIncreaseCapacity() {
        populateNodeConfigCache();
        NodeKey node1 =
                new NodeKey(new InstanceDetails.Id("node-1"), new InstanceDetails.Ip("1.2.3.4"));
        ModifyCacheMaxSizeAction.Builder builder =
                ModifyCacheMaxSizeAction.newBuilder(
                        node1, ResourceEnum.FIELD_DATA_CACHE, appContext, rcaConf);
        ModifyCacheMaxSizeAction fieldDataCacheIncrease = builder.increase(true).build();

        assertTrue(
                fieldDataCacheIncrease.getDesiredCacheMaxSizeInBytes()
                        > fieldDataCacheIncrease.getCurrentCacheMaxSizeInBytes());
        assertTrue(fieldDataCacheIncrease.isActionable());
        assertEquals(300 * 1_000, fieldDataCacheIncrease.coolOffPeriodInMillis());
        Assert.assertEquals(ResourceEnum.FIELD_DATA_CACHE, fieldDataCacheIncrease.getCacheType());
        assertEquals(1, fieldDataCacheIncrease.impactedNodes().size());

        Map<ImpactVector.Dimension, ImpactVector.Impact> impact =
                fieldDataCacheIncrease.impact().get(node1).getImpact();
        Assert.assertEquals(
                ImpactVector.Impact.INCREASES_PRESSURE, impact.get(ImpactVector.Dimension.HEAP));
        Assert.assertEquals(ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.CPU));
        Assert.assertEquals(
                ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.NETWORK));
        Assert.assertEquals(ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.RAM));
        Assert.assertEquals(ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.DISK));
    }

    @Test
    public void testDecreaseCapacity() {
        populateNodeConfigCache();
        NodeKey node1 =
                new NodeKey(new InstanceDetails.Id("node-1"), new InstanceDetails.Ip("1.2.3.4"));
        ModifyCacheMaxSizeAction.Builder builder =
                ModifyCacheMaxSizeAction.newBuilder(
                        node1, ResourceEnum.FIELD_DATA_CACHE, appContext, rcaConf);
        ModifyCacheMaxSizeAction fieldDataCache = builder.increase(false).build();
        assertTrue(
                fieldDataCache.getDesiredCacheMaxSizeInBytes()
                        < fieldDataCache.getCurrentCacheMaxSizeInBytes());
        assertTrue(fieldDataCache.isActionable());
        assertEquals(300 * 1_000, fieldDataCache.coolOffPeriodInMillis());
        Assert.assertEquals(ResourceEnum.FIELD_DATA_CACHE, fieldDataCache.getCacheType());
        assertEquals(1, fieldDataCache.impactedNodes().size());
        Map<ImpactVector.Dimension, ImpactVector.Impact> impact =
                fieldDataCache.impact().get(node1).getImpact();
        Assert.assertEquals(
                ImpactVector.Impact.DECREASES_PRESSURE, impact.get(ImpactVector.Dimension.HEAP));
        Assert.assertEquals(ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.CPU));
        Assert.assertEquals(
                ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.NETWORK));
        Assert.assertEquals(ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.RAM));
        Assert.assertEquals(ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.DISK));
    }

    @Test
    public void testBounds() throws Exception {
        final String configStr =
                "{"
                        + "\"action-config-settings\": { "
                        + "\"cache-settings\": { "
                        + "\"fielddata\": { "
                        + "\"upper-bound\": 0.75, "
                        + "\"lower-bound\": 0.55 "
                        + "}, "
                        + "\"shard-request\": { "
                        + "\"upper-bound\": 0.08, "
                        + "\"lower-bound\": 0.02 "
                        + "} "
                        + "} "
                        + "} "
                        + "}";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        NodeKey node =
                new NodeKey(new InstanceDetails.Id("node-2"), new InstanceDetails.Ip("4.5.6.7"));
        final long fieldDataUpperBoundInBytes =
                ModifyCacheMaxSizeAction.getThresholdInBytes(0.75, heapMaxSizeInBytes);
        final long shardRequestUpperBoundInBytes =
                ModifyCacheMaxSizeAction.getThresholdInBytes(0.08, heapMaxSizeInBytes);
        final long fieldDataLowerBoundInBytes =
                ModifyCacheMaxSizeAction.getThresholdInBytes(0.55, heapMaxSizeInBytes);
        final long shardRequestLowerBoundInBytes =
                ModifyCacheMaxSizeAction.getThresholdInBytes(0.02, heapMaxSizeInBytes);

        // Test Upper Bounds
        populateNodeConfigCache(
                node,
                heapMaxSizeInBytes,
                fieldDataUpperBoundInBytes,
                shardRequestUpperBoundInBytes);
        ModifyCacheMaxSizeAction action =
                ModifyCacheMaxSizeAction.newBuilder(
                                node, ResourceEnum.FIELD_DATA_CACHE, appContext, conf)
                        .increase(true)
                        .build();
        assertEquals(fieldDataUpperBoundInBytes, action.getDesiredCacheMaxSizeInBytes());
        assertFalse(action.isActionable());

        action =
                ModifyCacheMaxSizeAction.newBuilder(
                                node, ResourceEnum.SHARD_REQUEST_CACHE, appContext, conf)
                        .increase(true)
                        .build();
        assertEquals(shardRequestUpperBoundInBytes, action.getDesiredCacheMaxSizeInBytes());
        assertFalse(action.isActionable());

        // Test Lower Bounds
        populateNodeConfigCache(
                node,
                heapMaxSizeInBytes,
                fieldDataLowerBoundInBytes,
                shardRequestLowerBoundInBytes);
        action =
                ModifyCacheMaxSizeAction.newBuilder(
                                node, ResourceEnum.FIELD_DATA_CACHE, appContext, conf)
                        .increase(false)
                        .build();
        assertEquals(fieldDataLowerBoundInBytes, action.getDesiredCacheMaxSizeInBytes());
        assertFalse(action.isActionable());

        action =
                ModifyCacheMaxSizeAction.newBuilder(
                                node, ResourceEnum.SHARD_REQUEST_CACHE, appContext, conf)
                        .increase(false)
                        .build();
        assertEquals(shardRequestLowerBoundInBytes, action.getDesiredCacheMaxSizeInBytes());
        assertFalse(action.isActionable());
    }

    @Test
    public void testMinMaxOverrides() throws Exception {
        final String configStr =
                "{"
                        + "\"action-config-settings\": { "
                        + "\"cache-settings\": { "
                        + "\"fielddata\": { "
                        + "\"upper-bound\": 0.75, "
                        + "\"lower-bound\": 0.55 "
                        + "}, "
                        + "\"shard-request\": { "
                        + "\"upper-bound\": 0.08, "
                        + "\"lower-bound\": 0.02 "
                        + "} "
                        + "} "
                        + "} "
                        + "}";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        NodeKey node =
                new NodeKey(new InstanceDetails.Id("node-2"), new InstanceDetails.Ip("4.5.6.7"));
        final long fieldDataUpperBoundInBytes =
                ModifyCacheMaxSizeAction.getThresholdInBytes(0.75, heapMaxSizeInBytes);
        final long fieldDataCurrentInBytes =
                ModifyCacheMaxSizeAction.getThresholdInBytes(0.65, heapMaxSizeInBytes);
        final long fieldDataLowerBoundInBytes =
                ModifyCacheMaxSizeAction.getThresholdInBytes(0.55, heapMaxSizeInBytes);
        final long shardRequestUpperBoundInBytes =
                ModifyCacheMaxSizeAction.getThresholdInBytes(0.08, heapMaxSizeInBytes);
        final long shardRequestCurrentInBytes =
                ModifyCacheMaxSizeAction.getThresholdInBytes(0.05, heapMaxSizeInBytes);
        final long shardRequestLowerBoundInBytes =
                ModifyCacheMaxSizeAction.getThresholdInBytes(0.02, heapMaxSizeInBytes);
        populateNodeConfigCache(
                node, heapMaxSizeInBytes, fieldDataCurrentInBytes, shardRequestCurrentInBytes);

        // Test Max Override
        ModifyCacheMaxSizeAction action =
                ModifyCacheMaxSizeAction.newBuilder(
                                node, ResourceEnum.FIELD_DATA_CACHE, appContext, conf)
                        .setDesiredCacheMaxSizeToMax()
                        .build();
        assertEquals(fieldDataUpperBoundInBytes, action.getDesiredCacheMaxSizeInBytes());
        assertTrue(action.isActionable());

        action =
                ModifyCacheMaxSizeAction.newBuilder(
                                node, ResourceEnum.SHARD_REQUEST_CACHE, appContext, conf)
                        .setDesiredCacheMaxSizeToMax()
                        .build();
        assertEquals(shardRequestUpperBoundInBytes, action.getDesiredCacheMaxSizeInBytes());
        assertTrue(action.isActionable());

        // Test Min Override
        action =
                ModifyCacheMaxSizeAction.newBuilder(
                                node, ResourceEnum.FIELD_DATA_CACHE, appContext, conf)
                        .setDesiredCacheMaxSizeToMin()
                        .build();
        assertEquals(fieldDataLowerBoundInBytes, action.getDesiredCacheMaxSizeInBytes());
        assertTrue(action.isActionable());

        action =
                ModifyCacheMaxSizeAction.newBuilder(
                                node, ResourceEnum.SHARD_REQUEST_CACHE, appContext, conf)
                        .setDesiredCacheMaxSizeToMin()
                        .build();
        assertEquals(shardRequestLowerBoundInBytes, action.getDesiredCacheMaxSizeInBytes());
        assertTrue(action.isActionable());
    }

    @Test
    public void testMutedAction() {
        populateNodeConfigCache();
        NodeKey node1 =
                new NodeKey(new InstanceDetails.Id("node-1"), new InstanceDetails.Ip("1.2.3.4"));
        ModifyCacheMaxSizeAction.Builder builder =
                ModifyCacheMaxSizeAction.newBuilder(
                        node1, ResourceEnum.FIELD_DATA_CACHE, appContext, rcaConf);
        ModifyCacheMaxSizeAction modifyCacheSizeAction = builder.increase(true).build();

        appContext.updateMutedActions(ImmutableSet.of(modifyCacheSizeAction.name()));

        assertFalse(modifyCacheSizeAction.isActionable());
    }

    @Test
    public void testCacheMaxSizeNotPresent() {
        setNodeConfigCache(ResourceUtil.HEAP_MAX_SIZE, heapMaxSizeInBytes);
        NodeKey node1 =
                new NodeKey(new InstanceDetails.Id("node-1"), new InstanceDetails.Ip("1.2.3.4"));
        ModifyCacheMaxSizeAction.Builder builder =
                ModifyCacheMaxSizeAction.newBuilder(
                        node1, ResourceEnum.FIELD_DATA_CACHE, appContext, rcaConf);
        ModifyCacheMaxSizeAction fieldDataCacheNoAction = builder.increase(true).build();
        assertEquals(
                fieldDataCacheNoAction.getDesiredCacheMaxSizeInBytes(),
                fieldDataCacheNoAction.getCurrentCacheMaxSizeInBytes());
        assertFalse(fieldDataCacheNoAction.isActionable());
        assertEquals(300 * 1_000, fieldDataCacheNoAction.coolOffPeriodInMillis());
        Assert.assertEquals(ResourceEnum.FIELD_DATA_CACHE, fieldDataCacheNoAction.getCacheType());
        assertEquals(1, fieldDataCacheNoAction.impactedNodes().size());
        assertNoImpact(node1, fieldDataCacheNoAction);
    }

    @Test
    public void testHeapMaxSizeNotPresent() {
        setNodeConfigCache(ResourceUtil.FIELD_DATA_CACHE_MAX_SIZE, fieldDataCacheMaxSizeInBytes);
        NodeKey node1 =
                new NodeKey(new InstanceDetails.Id("node-1"), new InstanceDetails.Ip("1.2.3.4"));
        ModifyCacheMaxSizeAction.Builder builder =
                ModifyCacheMaxSizeAction.newBuilder(
                        node1, ResourceEnum.FIELD_DATA_CACHE, appContext, rcaConf);
        ModifyCacheMaxSizeAction fieldDataCacheNoAction = builder.increase(true).build();
        assertEquals(
                fieldDataCacheNoAction.getDesiredCacheMaxSizeInBytes(),
                fieldDataCacheNoAction.getCurrentCacheMaxSizeInBytes());
        assertFalse(fieldDataCacheNoAction.isActionable());
        assertEquals(300 * 1_000, fieldDataCacheNoAction.coolOffPeriodInMillis());
        Assert.assertEquals(ResourceEnum.FIELD_DATA_CACHE, fieldDataCacheNoAction.getCacheType());
        assertEquals(1, fieldDataCacheNoAction.impactedNodes().size());
        assertNoImpact(node1, fieldDataCacheNoAction);
    }

    @Test
    public void testUnboundedFielddataCache() throws Exception {
        final String configStr = "{}";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        NodeKey node =
                new NodeKey(new InstanceDetails.Id("node-1"), new InstanceDetails.Ip("1.2.3.4"));
        setNodeConfigCache(ResourceUtil.FIELD_DATA_CACHE_MAX_SIZE, -1);
        setNodeConfigCache(ResourceUtil.HEAP_MAX_SIZE, heapMaxSizeInBytes);
        ModifyCacheMaxSizeAction.Builder builder =
                ModifyCacheMaxSizeAction.newBuilder(
                        node, ResourceEnum.FIELD_DATA_CACHE, appContext, rcaConf);
        ModifyCacheMaxSizeAction increaseAction = builder.increase(true).build();
        assertTrue(increaseAction.isActionable());
        assertEquals(heapMaxSizeInBytes, increaseAction.getCurrentCacheMaxSizeInBytes());
        Assert.assertEquals(
                heapMaxSizeInBytes * CacheActionConfig.DEFAULT_FIELDDATA_CACHE_UPPER_BOUND,
                increaseAction.getDesiredCacheMaxSizeInBytes(),
                10);
        ModifyCacheMaxSizeAction decreaseAction = builder.increase(false).build();
        assertTrue(decreaseAction.isActionable());
        assertEquals(heapMaxSizeInBytes, decreaseAction.getCurrentCacheMaxSizeInBytes());
        assertEquals(
                heapMaxSizeInBytes * CacheActionConfig.DEFAULT_FIELDDATA_CACHE_UPPER_BOUND,
                decreaseAction.getDesiredCacheMaxSizeInBytes(),
                10);
    }

    @Test
    public void testSummary() {
        NodeKey node1 =
                new NodeKey(new InstanceDetails.Id("node-1"), new InstanceDetails.Ip("1.2.3.4"));
        populateNodeConfigCache();
        ModifyCacheMaxSizeAction.Builder builder =
                ModifyCacheMaxSizeAction.newBuilder(
                        node1, ResourceEnum.FIELD_DATA_CACHE, appContext, rcaConf);
        ModifyCacheMaxSizeAction modifyCacheMaxSizeAction = builder.increase(true).build();
        String summary = modifyCacheMaxSizeAction.summary();

        ModifyCacheMaxSizeAction objectFromSummary =
                ModifyCacheMaxSizeAction.fromSummary(summary, appContext);
        assertEquals(
                modifyCacheMaxSizeAction.getCurrentCacheMaxSizeInBytes(),
                objectFromSummary.getCurrentCacheMaxSizeInBytes());
        assertEquals(
                modifyCacheMaxSizeAction.getDesiredCacheMaxSizeInBytes(),
                objectFromSummary.getDesiredCacheMaxSizeInBytes());
        Assert.assertEquals(
                modifyCacheMaxSizeAction.getCacheType(), objectFromSummary.getCacheType());
    }

    private void assertNoImpact(NodeKey node, ModifyCacheMaxSizeAction modifyCacheSizeAction) {
        Map<ImpactVector.Dimension, ImpactVector.Impact> impact =
                modifyCacheSizeAction.impact().get(node).getImpact();
        Assert.assertEquals(ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.HEAP));
        Assert.assertEquals(ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.CPU));
        Assert.assertEquals(
                ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.NETWORK));
        Assert.assertEquals(ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.RAM));
        Assert.assertEquals(ImpactVector.Impact.NO_IMPACT, impact.get(ImpactVector.Dimension.DISK));
    }

    private void populateNodeConfigCache(
            NodeKey node,
            long heapMaxSizeInBytes,
            long fieldDataCacheMaxSizeInBytes,
            long shardRequestCacheMaxSizeInBytes) {
        appContext.getNodeConfigCache().put(node, ResourceUtil.HEAP_MAX_SIZE, heapMaxSizeInBytes);
        appContext
                .getNodeConfigCache()
                .put(node, ResourceUtil.FIELD_DATA_CACHE_MAX_SIZE, fieldDataCacheMaxSizeInBytes);
        appContext
                .getNodeConfigCache()
                .put(
                        node,
                        ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE,
                        shardRequestCacheMaxSizeInBytes);
    }

    private void populateNodeConfigCache() {
        NodeKey node =
                new NodeKey(new InstanceDetails.Id("node-1"), new InstanceDetails.Ip("1.2.3.4"));
        populateNodeConfigCache(
                node,
                heapMaxSizeInBytes,
                fieldDataCacheMaxSizeInBytes,
                shardRequestCacheMaxSizeInBytes);
    }

    private void setNodeConfigCache(final Resource resource, final long maxSizeInBytes) {
        appContext
                .getNodeConfigCache()
                .put(
                        new NodeKey(
                                new InstanceDetails.Id("node-1"),
                                new InstanceDetails.Ip("1.2.3.4")),
                        resource,
                        maxSizeInBytes);
    }
}
