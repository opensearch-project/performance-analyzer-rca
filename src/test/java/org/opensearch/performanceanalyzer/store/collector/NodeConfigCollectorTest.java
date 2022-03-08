/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.store.collector;


import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.GradleTaskForRca;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.NodeConfigFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Cache_Max_Size;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Heap_Max;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.ThreadPool_QueueCapacity;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.flow_units.MetricFlowUnitTestHelper;
import org.opensearch.performanceanalyzer.rca.store.collector.NodeConfigCollector;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;

@Category(GradleTaskForRca.class)
public class NodeConfigCollectorTest {
    private AppContext appContext;
    private NodeKey nodeKey;
    private ThreadPool_QueueCapacity threadPool_QueueCapacity;
    private Cache_Max_Size cacheMaxSize;
    private Heap_Max heapMax;
    private NodeConfigCollector nodeConfigCollector;

    @Before
    public void init() {
        threadPool_QueueCapacity = new ThreadPool_QueueCapacity();
        cacheMaxSize = new Cache_Max_Size(5);
        heapMax = new Heap_Max(5);
        nodeConfigCollector =
                new NodeConfigCollector(1, threadPool_QueueCapacity, cacheMaxSize, heapMax);

        ClusterDetailsEventProcessor clusterDetailsEventProcessor =
                new ClusterDetailsEventProcessor();
        ClusterDetailsEventProcessor.NodeDetails node1 =
                new ClusterDetailsEventProcessor.NodeDetails(
                        AllMetrics.NodeRole.DATA, "node1", "127.0.0.0", false);
        clusterDetailsEventProcessor.setNodesDetails(Collections.singletonList(node1));

        appContext = new AppContext();
        appContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);
        nodeConfigCollector.setAppContext(appContext);
        nodeKey = new NodeKey(appContext.getMyInstanceDetails());
    }

    /** generate flowunit and bind the flowunits it generate to metrics */
    @SuppressWarnings("unchecked")
    private void mockQueueFlowUnits(int writeQueueCapacity, int searchQueueCapacity) {
        MetricFlowUnit flowUnit =
                MetricFlowUnitTestHelper.createFlowUnit(
                        Arrays.asList(
                                AllMetrics.ThreadPoolDimension.THREAD_POOL_TYPE.toString(),
                                MetricsDB.MAX),
                        Arrays.asList(
                                AllMetrics.ThreadPoolType.WRITE.toString(),
                                String.valueOf(writeQueueCapacity)),
                        Arrays.asList(
                                AllMetrics.ThreadPoolType.SEARCH.toString(),
                                String.valueOf(searchQueueCapacity)));
        threadPool_QueueCapacity.setLocalFlowUnit(flowUnit);
    }

    /** generate flowunit and bind the flowunits it generate to metrics */
    @SuppressWarnings("unchecked")
    private void mockHeapMaxSizeFlowUnits(
            int heapMaxSize, int oldGenMaxSize, int edenMaxSize, int survivorMaxSize) {
        MetricFlowUnit flowUnit =
                MetricFlowUnitTestHelper.createFlowUnit(
                        Arrays.asList(AllMetrics.HeapDimension.MEM_TYPE.toString(), MetricsDB.MAX),
                        Arrays.asList(
                                AllMetrics.GCType.HEAP.toString(), String.valueOf(heapMaxSize)),
                        Arrays.asList(
                                AllMetrics.GCType.OLD_GEN.toString(),
                                String.valueOf(oldGenMaxSize)),
                        Arrays.asList(
                                AllMetrics.GCType.EDEN.toString(), String.valueOf(edenMaxSize)),
                        Arrays.asList(
                                AllMetrics.GCType.SURVIVOR.toString(),
                                String.valueOf(survivorMaxSize)));
        heapMax.setLocalFlowUnit(flowUnit);
    }

    @Test
    public void testQueueCapacityMetricNotExist() {
        threadPool_QueueCapacity.setLocalFlowUnit(MetricFlowUnit.generic());
        NodeConfigFlowUnit flowUnit = nodeConfigCollector.operate();
        Assert.assertTrue(flowUnit.isEmpty());
        Assert.assertFalse(flowUnit.hasConfig(ResourceUtil.SEARCH_QUEUE_CAPACITY));
        Assert.assertFalse(flowUnit.hasConfig(ResourceUtil.WRITE_QUEUE_CAPACITY));
    }

    @Test
    public void testCacheMaxSizeMetricNotExist() {
        cacheMaxSize.setLocalFlowUnit(MetricFlowUnit.generic());
        NodeConfigFlowUnit flowUnit = nodeConfigCollector.operate();
        Assert.assertTrue(flowUnit.isEmpty());
        Assert.assertFalse(flowUnit.hasConfig(ResourceUtil.FIELD_DATA_CACHE_MAX_SIZE));
        Assert.assertFalse(flowUnit.hasConfig(ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE));
    }

    @Test
    public void testHeapMaxSizeMetricNotExist() {
        heapMax.setLocalFlowUnit(MetricFlowUnit.generic());
        NodeConfigFlowUnit flowUnit = nodeConfigCollector.operate();
        Assert.assertTrue(flowUnit.isEmpty());
        Assert.assertFalse(flowUnit.hasConfig(ResourceUtil.HEAP_MAX_SIZE));
    }

    @Test
    public void testQueueCapacityCollection() {
        mockQueueFlowUnits(100, 200);
        NodeConfigFlowUnit flowUnit = nodeConfigCollector.operate();
        Assert.assertFalse(flowUnit.isEmpty());
        Assert.assertTrue(flowUnit.hasConfig(ResourceUtil.SEARCH_QUEUE_CAPACITY));
        Assert.assertEquals(200, flowUnit.readConfig(ResourceUtil.SEARCH_QUEUE_CAPACITY), 0.01);
        Assert.assertTrue(flowUnit.hasConfig(ResourceUtil.WRITE_QUEUE_CAPACITY));
        Assert.assertEquals(100, flowUnit.readConfig(ResourceUtil.WRITE_QUEUE_CAPACITY), 0.01);

        Assert.assertEquals(
                200,
                appContext.getNodeConfigCache().get(nodeKey, ResourceUtil.SEARCH_QUEUE_CAPACITY),
                0.01);
        Assert.assertEquals(
                100,
                appContext.getNodeConfigCache().get(nodeKey, ResourceUtil.WRITE_QUEUE_CAPACITY),
                0.01);
    }

    @Test
    public void testHeapMaxSizeCollection() {
        int heapMaxSize = 1000;
        int oldGenMaxSize = 7500;
        int edenMaxSize = 2000;
        int survivorMaxSize = 500;
        mockHeapMaxSizeFlowUnits(heapMaxSize, oldGenMaxSize, edenMaxSize, survivorMaxSize);
        NodeConfigFlowUnit flowUnit = nodeConfigCollector.operate();
        Assert.assertFalse(flowUnit.isEmpty());
        Assert.assertTrue(flowUnit.hasConfig(ResourceUtil.HEAP_MAX_SIZE));
        Assert.assertEquals(heapMaxSize, flowUnit.readConfig(ResourceUtil.HEAP_MAX_SIZE), 0.01);
        Assert.assertTrue(flowUnit.hasConfig(ResourceUtil.OLD_GEN_MAX_SIZE));
        Assert.assertEquals(
                oldGenMaxSize, flowUnit.readConfig(ResourceUtil.OLD_GEN_MAX_SIZE), 0.01);
        Assert.assertTrue(flowUnit.hasConfig(ResourceUtil.YOUNG_GEN_MAX_SIZE));
        int expectedYoungGenMaxSize = edenMaxSize + 2 * survivorMaxSize;
        Assert.assertEquals(
                expectedYoungGenMaxSize,
                flowUnit.readConfig(ResourceUtil.YOUNG_GEN_MAX_SIZE),
                0.01);
        // Sanity check on the appContext
        Assert.assertEquals(
                heapMaxSize,
                appContext.getNodeConfigCache().get(nodeKey, ResourceUtil.HEAP_MAX_SIZE),
                0.01);
        Assert.assertEquals(
                oldGenMaxSize,
                appContext.getNodeConfigCache().get(nodeKey, ResourceUtil.OLD_GEN_MAX_SIZE),
                0.01);
        Assert.assertEquals(
                expectedYoungGenMaxSize,
                appContext.getNodeConfigCache().get(nodeKey, ResourceUtil.YOUNG_GEN_MAX_SIZE),
                0.01);
    }
}
