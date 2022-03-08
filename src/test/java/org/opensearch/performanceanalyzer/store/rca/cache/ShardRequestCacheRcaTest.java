/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.store.rca.cache;

import static java.time.Instant.ofEpochMilli;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.GradleTaskForRca;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.MetricTestHelper;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.store.rca.cache.ShardRequestCacheRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;

@Category(GradleTaskForRca.class)
public class ShardRequestCacheRcaTest {

    private MetricTestHelper shardRequestCacheEvictions;
    private MetricTestHelper shardRequestCacheHits;
    private MetricTestHelper shardRequestCacheSize;
    private ShardRequestCacheRca shardRequestCacheRca;
    private List<String> columnName;
    private AppContext appContext;

    @Before
    public void init() throws Exception {
        shardRequestCacheEvictions = new MetricTestHelper(5);
        shardRequestCacheHits = new MetricTestHelper(5);
        shardRequestCacheSize = new MetricTestHelper(5);
        shardRequestCacheRca =
                new ShardRequestCacheRca(
                        1,
                        shardRequestCacheEvictions,
                        shardRequestCacheHits,
                        shardRequestCacheSize);
        columnName =
                Arrays.asList(
                        AllMetrics.ShardStatsDerivedDimension.INDEX_NAME.toString(),
                        AllMetrics.ShardStatsDerivedDimension.SHARD_ID.toString(),
                        MetricsDB.SUM,
                        MetricsDB.MAX);

        ClusterDetailsEventProcessor clusterDetailsEventProcessor =
                new ClusterDetailsEventProcessor();
        ClusterDetailsEventProcessor.NodeDetails node =
                new ClusterDetailsEventProcessor.NodeDetails(
                        AllMetrics.NodeRole.DATA, "node1", "127.0.0.1", false);
        clusterDetailsEventProcessor.setNodesDetails(Collections.singletonList(node));
        appContext = new AppContext();
        appContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);
        appContext
                .getNodeConfigCache()
                .put(
                        new NodeKey(
                                new InstanceDetails.Id("node1"),
                                new InstanceDetails.Ip("127.0.0.1")),
                        ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE,
                        0.0);
        shardRequestCacheRca.setAppContext(appContext);
    }

    /**
     * generate flowunit and bind the flowunit to metrics, sample record:
     *
     * <p>Eg:| IndexName | ShardID | SUM | AVG | MIN | MAX |
     * ------------------------------------------------- | .kibana_1 | 0 | 15.0 | 8.0 | 2.0 | 9.0 |
     */
    private void mockFlowUnits(int cacheEvictionCnt, int cacheHitCnt, double cacheSize) {
        shardRequestCacheEvictions.createTestFlowUnits(
                columnName,
                Arrays.asList(
                        "index_1",
                        "0",
                        String.valueOf(cacheEvictionCnt),
                        String.valueOf(cacheEvictionCnt)));
        shardRequestCacheHits.createTestFlowUnits(
                columnName,
                Arrays.asList(
                        "index_1", "0", String.valueOf(cacheHitCnt), String.valueOf(cacheHitCnt)));
        shardRequestCacheSize.createTestFlowUnits(
                columnName,
                Arrays.asList(
                        "index_1", "0", String.valueOf(cacheSize), String.valueOf(cacheSize)));
    }

    private void mockEmptyFlowUnits(double cacheSize) {
        shardRequestCacheEvictions.createEmptyTestFlowUnits();
        shardRequestCacheHits.createEmptyTestFlowUnits();
        shardRequestCacheSize.createTestFlowUnits(
                columnName,
                Arrays.asList(
                        "index_1", "0", String.valueOf(cacheSize), String.valueOf(cacheSize)));
    }

    @Test
    public void testShardRequestCache() {
        ResourceFlowUnit<HotNodeSummary> flowUnit;
        Clock constantClock = Clock.fixed(ofEpochMilli(0), ZoneId.systemDefault());

        // TimeWindow 1 of size 300sec
        mockFlowUnits(0, 0, 0.0);
        shardRequestCacheRca.setClock(constantClock);
        flowUnit = shardRequestCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        appContext
                .getNodeConfigCache()
                .put(
                        new NodeKey(
                                new InstanceDetails.Id("node1"),
                                new InstanceDetails.Ip("127.0.0.1")),
                        ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE,
                        3.0);
        mockFlowUnits(0, 0, 4.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(3)));
        flowUnit = shardRequestCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        mockFlowUnits(1, 0, 4.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(4)));
        flowUnit = shardRequestCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        // TimeWindow 2 of size 300sec
        appContext
                .getNodeConfigCache()
                .put(
                        new NodeKey(
                                new InstanceDetails.Id("node1"),
                                new InstanceDetails.Ip("127.0.0.1")),
                        ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE,
                        0.0);
        mockFlowUnits(1, 1, 0.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(7)));
        flowUnit = shardRequestCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        appContext
                .getNodeConfigCache()
                .put(
                        new NodeKey(
                                new InstanceDetails.Id("node1"),
                                new InstanceDetails.Ip("127.0.0.1")),
                        ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE,
                        3.0);
        mockFlowUnits(1, 1, 4.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(10)));
        flowUnit = shardRequestCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        // TimeWindow 3 of size 300sec
        mockFlowUnits(0, 0, 4.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(12)));
        flowUnit = shardRequestCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        mockFlowUnits(1, 0, 4.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(14)));
        flowUnit = shardRequestCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        mockFlowUnits(1, 0, 4.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(14)));
        flowUnit = shardRequestCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        // TimeWindow 4 of size 300sec
        appContext
                .getNodeConfigCache()
                .put(
                        new NodeKey(
                                new InstanceDetails.Id("node1"),
                                new InstanceDetails.Ip("127.0.0.1")),
                        ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE,
                        0.0);
        mockFlowUnits(0, 1, 0.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(17)));
        flowUnit = shardRequestCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        mockFlowUnits(1, 1, 0.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(20)));
        flowUnit = shardRequestCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        appContext
                .getNodeConfigCache()
                .put(
                        new NodeKey(
                                new InstanceDetails.Id("node1"),
                                new InstanceDetails.Ip("127.0.0.1")),
                        ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE,
                        3.0);
        mockFlowUnits(1, 1, 4.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(20)));
        flowUnit = shardRequestCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        // TimeWindow 4 of size 300sec
        mockFlowUnits(1, 1, 2.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(25)));
        flowUnit = shardRequestCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        mockFlowUnits(1, 1, 4.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(25)));
        flowUnit = shardRequestCacheRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());

        Assert.assertTrue(flowUnit.hasResourceSummary());
        HotNodeSummary nodeSummary = flowUnit.getSummary();
        Assert.assertEquals(1, nodeSummary.getNestedSummaryList().size());
        Assert.assertEquals(1, nodeSummary.getHotResourceSummaryList().size());
        HotResourceSummary resourceSummary = nodeSummary.getHotResourceSummaryList().get(0);
        Assert.assertEquals(
                ResourceUtil.SHARD_REQUEST_CACHE_EVICTION, resourceSummary.getResource());
        Assert.assertEquals(0.01, 6.0, resourceSummary.getValue());

        // TimeWindow 5 of size 300sec
        mockFlowUnits(0, 0, 4.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(27)));
        flowUnit = shardRequestCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        mockFlowUnits(0, 0, 2.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(27)));
        flowUnit = shardRequestCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());
    }

    @Test
    public void testShardRequestCacheEmptyFU() {
        ResourceFlowUnit<HotNodeSummary> flowUnit;
        Clock constantClock = Clock.fixed(ofEpochMilli(0), ZoneId.systemDefault());
        appContext
                .getNodeConfigCache()
                .put(
                        new NodeKey(
                                new InstanceDetails.Id("node1"),
                                new InstanceDetails.Ip("127.0.0.1")),
                        ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE,
                        3.0);

        mockFlowUnits(0, 0, 0.0);
        shardRequestCacheRca.setClock(constantClock);
        Assert.assertFalse(shardRequestCacheRca.operate().getResourceContext().isUnhealthy());

        mockFlowUnits(1, 0, 4.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(3)));
        Assert.assertFalse(shardRequestCacheRca.operate().getResourceContext().isUnhealthy());

        mockFlowUnits(1, 0, 4.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(4)));
        Assert.assertFalse(shardRequestCacheRca.operate().getResourceContext().isUnhealthy());

        mockFlowUnits(1, 1, 4.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(7)));
        Assert.assertFalse(shardRequestCacheRca.operate().getResourceContext().isUnhealthy());

        mockFlowUnits(1, 1, 4.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(10)));
        Assert.assertFalse(shardRequestCacheRca.operate().getResourceContext().isUnhealthy());

        mockFlowUnits(1, 1, 4.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(12)));
        Assert.assertTrue(shardRequestCacheRca.operate().getResourceContext().isUnhealthy());

        // Put empty flow unit for Eviction metrics
        mockEmptyFlowUnits(7.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(14)));
        Assert.assertTrue(shardRequestCacheRca.operate().getResourceContext().isUnhealthy());

        mockEmptyFlowUnits(7.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(17)));
        flowUnit = shardRequestCacheRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());

        mockEmptyFlowUnits(7.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(20)));
        Assert.assertTrue(shardRequestCacheRca.operate().getResourceContext().isUnhealthy());

        // After empty flow unit in 3rd execution cycle, RCA will mark the hasEviction as false
        // and result in Healthy context
        mockEmptyFlowUnits(7.0);
        shardRequestCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(25)));
        flowUnit = shardRequestCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());
    }
}
