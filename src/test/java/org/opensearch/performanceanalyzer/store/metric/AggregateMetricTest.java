/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.store.metric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import org.junit.Test;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.CPU_Utilization;
import org.opensearch.performanceanalyzer.rca.framework.core.Queryable;
import org.opensearch.performanceanalyzer.rca.spec.MetricsDBProviderTestHelper;
import org.opensearch.performanceanalyzer.rca.store.metric.AggregateMetric;

public class AggregateMetricTest {
    private static final String TABLE_NAME = CPU_Utilization.NAME;

    @Test
    public void testGroupByOneColumn() throws Exception {
        Queryable queryable = new MetricsDBProviderTestHelper(false);

        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(TABLE_NAME, Arrays.asList("shard1", "index3", "bulk", "primary"), 1);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(TABLE_NAME, Arrays.asList("shard2", "index3", "bulk", "primary"), 1);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(TABLE_NAME, Arrays.asList("shard1", "index3", "bulk", "primary"), 1);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(TABLE_NAME, Arrays.asList("shard2", "index3", "other", "primary"), 3);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(TABLE_NAME, Arrays.asList("shard1", "index3", "other", "primary"), 3);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(TABLE_NAME, Arrays.asList("shard2", "index3", "other", "primary"), 3);
        Metric testMetric =
                new AggregateMetric(
                        1,
                        TABLE_NAME,
                        AggregateMetric.AggregateFunction.SUM,
                        MetricsDB.AVG,
                        AllMetrics.CommonDimension.OPERATION.toString());
        MetricFlowUnit flowUnit = testMetric.gather(queryable);
        assertFalse(flowUnit.getData() == null || flowUnit.getData().isEmpty());
        assertEquals("other", flowUnit.getData().get(0).getValue(0, String.class));
        assertEquals("9.0", flowUnit.getData().get(0).getValue(1, String.class));
        assertEquals("bulk", flowUnit.getData().get(1).getValue(0, String.class));
        assertEquals("3.0", flowUnit.getData().get(1).getValue(1, String.class));
    }

    @Test
    public void testGroupByTwoColumns() throws Exception {
        Queryable queryable = new MetricsDBProviderTestHelper(false);

        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(TABLE_NAME, Arrays.asList("shard1", "index3", "bulk", "primary"), 4);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(TABLE_NAME, Arrays.asList("shard2", "index3", "bulk", "primary"), 1);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(TABLE_NAME, Arrays.asList("shard1", "index3", "bulk", "primary"), 1);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(TABLE_NAME, Arrays.asList("shard2", "index3", "other", "primary"), 3);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(TABLE_NAME, Arrays.asList("shard1", "index3", "other", "primary"), 3);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(TABLE_NAME, Arrays.asList("shard3", "index3", "other", "primary"), 10);
        Metric testMetric =
                new AggregateMetric(
                        1,
                        TABLE_NAME,
                        AggregateMetric.AggregateFunction.SUM,
                        MetricsDB.AVG,
                        AllMetrics.CommonDimension.SHARD_ID.toString(),
                        AllMetrics.CommonDimension.OPERATION.toString());
        MetricFlowUnit flowUnit = testMetric.gather(queryable);
        assertFalse(flowUnit.getData() == null || flowUnit.getData().isEmpty());
        assertEquals("shard3", flowUnit.getData().get(0).get(0, String.class));
        assertEquals("other", flowUnit.getData().get(0).get(1, String.class));
        assertEquals("10.0", flowUnit.getData().get(0).get(2, String.class));
        assertEquals("shard1", flowUnit.getData().get(1).get(0, String.class));
        assertEquals("bulk", flowUnit.getData().get(1).get(1, String.class));
        assertEquals("5.0", flowUnit.getData().get(1).get(2, String.class));
    }

    @Test
    public void testOrderByMax() throws Exception {
        Queryable queryable = new MetricsDBProviderTestHelper(false);

        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(TABLE_NAME, Arrays.asList("shard1", "index3", "bulk", "primary"), 1);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(TABLE_NAME, Arrays.asList("shard2", "index3", "bulk", "primary"), 4);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(TABLE_NAME, Arrays.asList("shard1", "index3", "bulk", "primary"), 1);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(TABLE_NAME, Arrays.asList("shard2", "index3", "other", "primary"), 3);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(TABLE_NAME, Arrays.asList("shard1", "index3", "other", "primary"), 3);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(TABLE_NAME, Arrays.asList("shard2", "index3", "other", "primary"), 5);
        Metric testMetric =
                new AggregateMetric(
                        1,
                        TABLE_NAME,
                        AggregateMetric.AggregateFunction.MAX,
                        MetricsDB.AVG,
                        AllMetrics.CommonDimension.OPERATION.toString());
        MetricFlowUnit flowUnit = testMetric.gather(queryable);
        assertFalse(flowUnit.getData() == null || flowUnit.getData().isEmpty());
        assertEquals("other", flowUnit.getData().get(0).get(0, String.class));
        assertEquals("5.0", flowUnit.getData().get(0).get(1, String.class));
        assertEquals("bulk", flowUnit.getData().get(1).get(0, String.class));
        assertEquals("4.0", flowUnit.getData().get(1).get(1, String.class));
    }
}
