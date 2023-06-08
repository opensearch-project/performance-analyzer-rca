/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.store.rca.hotshard;

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
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.MetricTestHelper;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotShardSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericSummary;
import org.opensearch.performanceanalyzer.rca.store.rca.hotshard.HotShardRca;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;

@Category(GradleTaskForRca.class)
public class HotShardRcaTest {

    private HotShardRcaX hotShardRcaX;
    private MetricTestHelper cpuUtilization;
    private List<String> columnName;

    private enum index {
        index_1,
        index_2
    }

    @Before
    public void setup() {
        cpuUtilization = new MetricTestHelper(5);
        hotShardRcaX = new HotShardRcaX(5, 1, cpuUtilization);
        columnName =
                Arrays.asList(
                        AllMetrics.CommonDimension.INDEX_NAME.toString(),
                        AllMetrics.CommonDimension.SHARD_ID.toString(),
                        MetricsDB.SUM);

        ClusterDetailsEventProcessor clusterDetailsEventProcessor =
                new ClusterDetailsEventProcessor();
        clusterDetailsEventProcessor.setNodesDetails(
                Collections.singletonList(
                        new ClusterDetailsEventProcessor.NodeDetails(
                                AllMetrics.NodeRole.DATA, "node1", "127.0.0.1", false)));
        AppContext appContext = new AppContext();
        appContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);
        hotShardRcaX.setAppContext(appContext);
    }

    // 1. No Flow Units received
    @Test
    public void testOperateForMissingFlowUnits() {
        cpuUtilization = null;

        ResourceFlowUnit flowUnit = hotShardRcaX.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());
    }

    // 2. Empty Flow Units received
    @Test
    public void testOperateForEmptyFlowUnits() {
        cpuUtilization.createTestFlowUnits(columnName, Collections.emptyList());

        ResourceFlowUnit flowUnit = hotShardRcaX.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());
    }

    // 1. No Flow Units received/generated on cluster_manager
    @Test
    public void testOperate() {
        Clock constantClock = Clock.fixed(ofEpochMilli(0), ZoneId.systemDefault());

        // ts = 0
        // index = index_1, shard = shard_1, cpuUtilization = 0
        cpuUtilization.createTestFlowUnits(
                columnName, Arrays.asList(index.index_1.toString(), "1", String.valueOf(0)));

        hotShardRcaX.setClock(constantClock);
        ResourceFlowUnit flowUnit = hotShardRcaX.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        // ts = 1
        // index = index_1, shard = shard_1, cpuUtilization = 0.005
        cpuUtilization.createTestFlowUnits(
                columnName, Arrays.asList(index.index_1.toString(), "1", String.valueOf(0.005)));

        hotShardRcaX.setClock(Clock.offset(constantClock, Duration.ofSeconds(1)));
        flowUnit = hotShardRcaX.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        // ts = 2
        // index = index_1, shard = shard_1, cpuUtilization = 0.75
        cpuUtilization.createTestFlowUnits(
                columnName, Arrays.asList(index.index_1.toString(), "1", String.valueOf(0.75)));

        hotShardRcaX.setClock(Clock.offset(constantClock, Duration.ofSeconds(2)));
        flowUnit = hotShardRcaX.operate();
        HotNodeSummary summary1 = (HotNodeSummary) flowUnit.getSummary();
        List<GenericSummary> hotShardSummaryList1 = summary1.getNestedSummaryList();

        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        Assert.assertEquals(1, hotShardSummaryList1.size());

        HotShardSummary hotShardSummary1 = (HotShardSummary) hotShardSummaryList1.get(0);
        Assert.assertEquals("1", hotShardSummary1.getShardId());
        Assert.assertEquals(index.index_1.toString(), hotShardSummary1.getIndexName());
        Assert.assertEquals("node1", hotShardSummary1.getNodeId());

        // ts = 3
        // index = index_1, shard = shard_2, cpuUtilization = 0.75
        // index = index_1, shard = shard_3, cpuUtilization = 0.45

        cpuUtilization.createTestFlowUnitsWithMultipleRows(
                columnName,
                Arrays.asList(
                        Arrays.asList(index.index_1.toString(), "2", String.valueOf(0.75)),
                        Arrays.asList(index.index_1.toString(), "3", String.valueOf(0.45))));

        hotShardRcaX.setClock(Clock.offset(constantClock, Duration.ofSeconds(3)));

        flowUnit = hotShardRcaX.operate();
        HotNodeSummary summary2 = (HotNodeSummary) flowUnit.getSummary();
        List<GenericSummary> hotShardSummaryList2 = summary2.getNestedSummaryList();

        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        Assert.assertEquals(2, hotShardSummaryList2.size());

        HotShardSummary hotShardSummary2 = (HotShardSummary) hotShardSummaryList2.get(0);
        HotShardSummary hotShardSummary3 = (HotShardSummary) hotShardSummaryList2.get(1);
        Assert.assertEquals(index.index_1.toString(), hotShardSummary2.getIndexName());
        Assert.assertEquals("2", hotShardSummary2.getShardId());
        Assert.assertEquals("node1", hotShardSummary2.getNodeId());

        Assert.assertEquals(index.index_1.toString(), hotShardSummary3.getIndexName());
        Assert.assertEquals("3", hotShardSummary3.getShardId());
        Assert.assertEquals("node1", hotShardSummary3.getNodeId());
    }

    private static class HotShardRcaX extends HotShardRca {
        public <M extends Metric> HotShardRcaX(
                final long evaluationIntervalSeconds, final int rcaPeriod, final M cpuUtilization) {
            super(evaluationIntervalSeconds, rcaPeriod, cpuUtilization);
        }

        public void setClock(Clock clock) {
            this.clock = clock;
        }
    }
}
