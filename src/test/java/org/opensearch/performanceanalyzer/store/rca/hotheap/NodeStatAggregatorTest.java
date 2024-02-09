/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.store.rca.hotheap;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.GradleTaskForRca;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.MetricTestHelper;
import org.opensearch.performanceanalyzer.rca.store.rca.hotheap.NodeStatAggregator;

@Category(GradleTaskForRca.class)
public class NodeStatAggregatorTest {

    private MetricTestHelper nodeStat;
    private NodeStatAggregator nodeStatAggregator;

    @Before
    public void setup() {
        this.nodeStat = new MetricTestHelper(5);
        this.nodeStatAggregator = new NodeStatAggregator(nodeStat);
    }

    @Test
    public void testCollect() {
        List<String> columnName =
                Arrays.asList(
                        AllMetrics.CommonDimension.INDEX_NAME.toString(),
                        AllMetrics.CommonDimension.SHARD_ID.toString(),
                        MetricsDB.MAX);

        nodeStat.createTestFlowUnits(columnName, Arrays.asList("index1", "1", "5"));
        nodeStatAggregator.collect(0);
        Assert.assertEquals(5, nodeStatAggregator.getSum());

        nodeStat.createTestFlowUnits(columnName, Arrays.asList("index1", "2", "3"));
        nodeStatAggregator.collect(TimeUnit.MINUTES.toMillis(3));
        Assert.assertEquals(8, nodeStatAggregator.getSum());

        nodeStat.createTestFlowUnits(columnName, Arrays.asList("index2", "1", "10"));
        nodeStatAggregator.collect(TimeUnit.MINUTES.toMillis(8));
        Assert.assertEquals(18, nodeStatAggregator.getSum());

        nodeStat.createTestFlowUnits(columnName, Arrays.asList("index1", "2", "1"));
        nodeStatAggregator.collect(TimeUnit.MINUTES.toMillis(12));
        Assert.assertEquals(16, nodeStatAggregator.getSum());

        // purge the hash after 30min
        nodeStat.createTestFlowUnits(columnName, Arrays.asList("index2", "2", "10"));
        nodeStatAggregator.collect(TimeUnit.MINUTES.toMillis(32));
        Assert.assertEquals(21, nodeStatAggregator.getSum());
    }
}
