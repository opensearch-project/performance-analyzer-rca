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
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.GradleTaskForRca;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.MetricTestHelper;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.store.rca.cache.FieldDataCacheRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;

@Category(GradleTaskForRca.class)
public class FieldDataCacheRcaTest {

    private MetricTestHelper fieldDataCacheEvictions;
    private MetricTestHelper fieldDataCacheWeight;
    private FieldDataCacheRca fieldDataCacheRca;
    private List<String> columnName;
    private AppContext appContext;

    @Before
    public void init() throws Exception {
        fieldDataCacheEvictions = new MetricTestHelper(5);
        fieldDataCacheWeight = new MetricTestHelper(5);
        fieldDataCacheRca = new FieldDataCacheRca(1, fieldDataCacheEvictions, fieldDataCacheWeight);
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
                        ResourceUtil.FIELD_DATA_CACHE_MAX_SIZE,
                        5.0);
        fieldDataCacheRca.setAppContext(appContext);
    }

    /**
     * generate flowunit and bind the flowunit to metrics, sample record:
     *
     * <p>Eg:| IndexName | ShardID | SUM | AVG | MIN | MAX |
     * ------------------------------------------------- | .kibana_1 | 0 | 15.0 | 8.0 | 2.0 | 9.0 |
     */
    private void mockFlowUnits(int cacheEvictionCnt, double cacheWeight) {
        fieldDataCacheEvictions.createTestFlowUnits(
                columnName,
                Arrays.asList(
                        "index_1",
                        "0",
                        String.valueOf(cacheEvictionCnt),
                        String.valueOf(cacheEvictionCnt)));
        fieldDataCacheWeight.createTestFlowUnits(
                columnName,
                Arrays.asList(
                        "index_1", "0", String.valueOf(cacheWeight), String.valueOf(cacheWeight)));
    }

    private void mockEmptyEvictFlowUnits(double cacheWeight) {
        fieldDataCacheEvictions.createEmptyTestFlowUnits();
        fieldDataCacheWeight.createTestFlowUnits(
                columnName,
                Arrays.asList(
                        "index_1", "0", String.valueOf(cacheWeight), String.valueOf(cacheWeight)));
    }

    @Test
    public void testFieldDataCache() {
        ResourceFlowUnit<HotNodeSummary> flowUnit;
        Clock constantClock = Clock.fixed(ofEpochMilli(0), ZoneId.systemDefault());

        // TimeWindow 1 of size 300sec
        mockFlowUnits(0, 1.0);
        fieldDataCacheRca.setClock(constantClock);
        flowUnit = fieldDataCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        mockFlowUnits(0, 1.0);
        fieldDataCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(3)));
        flowUnit = fieldDataCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        mockFlowUnits(1, 1.0);
        fieldDataCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(4)));
        flowUnit = fieldDataCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        // TimeWindow 2 of size 300sec
        mockFlowUnits(1, 7.0);
        fieldDataCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(7)));
        flowUnit = fieldDataCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        mockFlowUnits(1, 1.0);
        fieldDataCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(10)));
        flowUnit = fieldDataCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        // TimeWindow 3 of size 300sec
        mockFlowUnits(1, 7.0);
        fieldDataCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(12)));
        flowUnit = fieldDataCacheRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());

        Assert.assertTrue(flowUnit.hasResourceSummary());
        HotNodeSummary nodeSummary = flowUnit.getSummary();
        Assert.assertEquals(1, nodeSummary.getNestedSummaryList().size());
        Assert.assertEquals(1, nodeSummary.getHotResourceSummaryList().size());
        HotResourceSummary resourceSummary = nodeSummary.getHotResourceSummaryList().get(0);
        Assert.assertEquals(ResourceUtil.FIELD_DATA_CACHE_EVICTION, resourceSummary.getResource());
        Assert.assertEquals(0.01, 6.0, resourceSummary.getValue());

        mockFlowUnits(0, 1.0);
        fieldDataCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(14)));
        flowUnit = fieldDataCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        // TimeWindow 4 of size 300sec
        mockFlowUnits(0, 7.0);
        fieldDataCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(17)));
        flowUnit = fieldDataCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());
    }

    @Test
    public void testFieldDataCacheEmptyFU() {
        ResourceFlowUnit<HotNodeSummary> flowUnit;
        Clock constantClock = Clock.fixed(ofEpochMilli(0), ZoneId.systemDefault());

        mockFlowUnits(1, 1.0);
        fieldDataCacheRca.setClock(constantClock);
        Assert.assertFalse(fieldDataCacheRca.operate().getResourceContext().isUnhealthy());

        mockFlowUnits(1, 1.0);
        fieldDataCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(3)));
        Assert.assertFalse(fieldDataCacheRca.operate().getResourceContext().isUnhealthy());

        mockFlowUnits(1, 1.0);
        fieldDataCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(4)));
        Assert.assertFalse(fieldDataCacheRca.operate().getResourceContext().isUnhealthy());

        mockFlowUnits(1, 7.0);
        fieldDataCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(7)));
        Assert.assertTrue(fieldDataCacheRca.operate().getResourceContext().isUnhealthy());

        // Put empty flow unit for Eviction metrics
        mockEmptyEvictFlowUnits(7.0);
        fieldDataCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(10)));
        Assert.assertTrue(fieldDataCacheRca.operate().getResourceContext().isUnhealthy());

        mockEmptyEvictFlowUnits(7.0);
        fieldDataCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(12)));
        flowUnit = fieldDataCacheRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());

        mockEmptyEvictFlowUnits(7.0);
        fieldDataCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(14)));
        Assert.assertTrue(fieldDataCacheRca.operate().getResourceContext().isUnhealthy());

        // After empty flow unit in 3rd execution cycle, RCA will mark the hasEviction as false
        // and result in Healthy context
        mockEmptyEvictFlowUnits(7.0);
        fieldDataCacheRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(17)));
        flowUnit = fieldDataCacheRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());
    }
}
