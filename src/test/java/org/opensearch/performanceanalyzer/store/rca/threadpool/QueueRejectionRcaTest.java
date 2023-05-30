/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.store.rca.threadpool;

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
import org.opensearch.performanceanalyzer.rca.store.rca.threadpool.QueueRejectionRca;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;

@Category(GradleTaskForRca.class)
public class QueueRejectionRcaTest {

    private MetricTestHelper threadPool_RejectedReqs;
    private QueueRejectionRca queueRejectionRca;
    private List<String> columnName;

    /** generate flowunit and bind the flowunits it generate to metrics */
    private void mockFlowUnits(int writeRejectCnt, int searchRejectCnt) {
        threadPool_RejectedReqs.createTestFlowUnitsWithMultipleRows(
                columnName,
                Arrays.asList(
                        Arrays.asList(
                                AllMetrics.ThreadPoolType.WRITE.toString(),
                                String.valueOf(writeRejectCnt)),
                        Arrays.asList(
                                AllMetrics.ThreadPoolType.SEARCH.toString(),
                                String.valueOf(searchRejectCnt))));
    }

    private void mockEmptyFlowUnits() {
        threadPool_RejectedReqs.createEmptyTestFlowUnits();
        threadPool_RejectedReqs.createEmptyTestFlowUnits();
    }

    @Before
    public void init() throws Exception {
        threadPool_RejectedReqs = new MetricTestHelper(5);
        queueRejectionRca = new QueueRejectionRca(1, threadPool_RejectedReqs);
        columnName =
                Arrays.asList(
                        AllMetrics.ThreadPoolDimension.THREAD_POOL_TYPE.toString(), MetricsDB.MAX);

        ClusterDetailsEventProcessor clusterDetailsEventProcessor =
                new ClusterDetailsEventProcessor();
        clusterDetailsEventProcessor.setNodesDetails(
                Collections.singletonList(
                        new ClusterDetailsEventProcessor.NodeDetails(
                                AllMetrics.NodeRole.DATA, "node1", "127.0.0.1", false)));
        AppContext appContext = new AppContext();
        appContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);

        queueRejectionRca.setAppContext(appContext);
    }

    @Test
    public void testWriteQueueOnly() {
        ResourceFlowUnit<HotNodeSummary> flowUnit;
        Clock constantClock = Clock.fixed(ofEpochMilli(0), ZoneId.systemDefault());

        mockFlowUnits(0, 0);
        queueRejectionRca.setClock(constantClock);
        flowUnit = queueRejectionRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        mockFlowUnits(0, 0);
        queueRejectionRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(3)));
        flowUnit = queueRejectionRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        mockFlowUnits(1, 0);
        queueRejectionRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(4)));
        flowUnit = queueRejectionRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        mockFlowUnits(1, 0);
        queueRejectionRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(7)));
        flowUnit = queueRejectionRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        mockFlowUnits(1, 0);
        queueRejectionRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(10)));
        flowUnit = queueRejectionRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());

        Assert.assertTrue(flowUnit.hasResourceSummary());
        HotNodeSummary nodeSummary = flowUnit.getSummary();
        Assert.assertEquals(1, nodeSummary.getNestedSummaryList().size());
        Assert.assertEquals(1, nodeSummary.getHotResourceSummaryList().size());
        HotResourceSummary resourceSummary = nodeSummary.getHotResourceSummaryList().get(0);
        Assert.assertEquals(ResourceUtil.WRITE_QUEUE_REJECTION, resourceSummary.getResource());
        Assert.assertEquals(0.01, 6.0, resourceSummary.getValue());

        mockFlowUnits(0, 0);
        queueRejectionRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(12)));
        flowUnit = queueRejectionRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());
    }

    @Test
    public void testWriteAndSearchQueues() {
        ResourceFlowUnit<HotNodeSummary> flowUnit;
        Clock constantClock = Clock.fixed(ofEpochMilli(0), ZoneId.systemDefault());

        mockFlowUnits(0, 0);
        queueRejectionRca.setClock(constantClock);
        flowUnit = queueRejectionRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        mockFlowUnits(0, 1);
        queueRejectionRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(3)));
        flowUnit = queueRejectionRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        mockFlowUnits(1, 1);
        queueRejectionRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(5)));
        flowUnit = queueRejectionRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        mockFlowUnits(1, 1);
        queueRejectionRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(12)));
        flowUnit = queueRejectionRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());

        Assert.assertTrue(flowUnit.hasResourceSummary());
        HotNodeSummary nodeSummary = flowUnit.getSummary();
        Assert.assertEquals(2, nodeSummary.getNestedSummaryList().size());
        Assert.assertEquals(2, nodeSummary.getHotResourceSummaryList().size());
        HotResourceSummary resourceSummary = nodeSummary.getHotResourceSummaryList().get(1);
        Assert.assertEquals(ResourceUtil.SEARCH_QUEUE_REJECTION, resourceSummary.getResource());
        Assert.assertEquals(0.01, 9.0, resourceSummary.getValue());
        resourceSummary = nodeSummary.getHotResourceSummaryList().get(0);
        Assert.assertEquals(ResourceUtil.WRITE_QUEUE_REJECTION, resourceSummary.getResource());
        Assert.assertEquals(0.01, 7.0, resourceSummary.getValue());
    }

    @Test
    public void testWriteAndSearchQueuesEmptyFU() {
        ResourceFlowUnit<HotNodeSummary> flowUnit;
        Clock constantClock = Clock.fixed(ofEpochMilli(0), ZoneId.systemDefault());

        mockFlowUnits(0, 0);
        queueRejectionRca.setClock(constantClock);
        Assert.assertFalse(queueRejectionRca.operate().getResourceContext().isUnhealthy());

        mockFlowUnits(0, 1);
        queueRejectionRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(3)));
        Assert.assertFalse(queueRejectionRca.operate().getResourceContext().isUnhealthy());

        mockFlowUnits(1, 1);
        queueRejectionRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(5)));
        Assert.assertFalse(queueRejectionRca.operate().getResourceContext().isUnhealthy());

        mockFlowUnits(1, 1);
        queueRejectionRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(12)));
        flowUnit = queueRejectionRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());

        Assert.assertTrue(flowUnit.hasResourceSummary());
        HotNodeSummary nodeSummary = flowUnit.getSummary();
        Assert.assertEquals(2, nodeSummary.getNestedSummaryList().size());
        Assert.assertEquals(2, nodeSummary.getHotResourceSummaryList().size());
        HotResourceSummary resourceSummary = nodeSummary.getHotResourceSummaryList().get(1);
        Assert.assertEquals(ResourceUtil.SEARCH_QUEUE_REJECTION, resourceSummary.getResource());
        Assert.assertEquals(0.01, 9.0, resourceSummary.getValue());
        resourceSummary = nodeSummary.getHotResourceSummaryList().get(0);
        Assert.assertEquals(ResourceUtil.WRITE_QUEUE_REJECTION, resourceSummary.getResource());
        Assert.assertEquals(0.01, 7.0, resourceSummary.getValue());

        // Put empty flow unit for Eviction metrics
        mockEmptyFlowUnits();
        queueRejectionRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(14)));
        Assert.assertTrue(queueRejectionRca.operate().getResourceContext().isUnhealthy());

        mockEmptyFlowUnits();
        queueRejectionRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(17)));
        flowUnit = queueRejectionRca.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());

        mockEmptyFlowUnits();
        queueRejectionRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(20)));
        Assert.assertTrue(queueRejectionRca.operate().getResourceContext().isUnhealthy());

        // After empty flow unit in 3rd execution cycle, RCA will mark the hasEviction as false
        // and result in Healthy context
        mockEmptyFlowUnits();
        queueRejectionRca.setClock(Clock.offset(constantClock, Duration.ofMinutes(25)));
        flowUnit = queueRejectionRca.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());
    }
}
