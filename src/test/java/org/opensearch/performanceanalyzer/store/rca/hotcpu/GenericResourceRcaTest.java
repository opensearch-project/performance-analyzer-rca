/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.store.rca.hotcpu;

import static java.time.Instant.ofEpochMilli;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.GradleTaskForRca;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.MetricTestHelper;
import org.opensearch.performanceanalyzer.rca.store.rca.hot_node.HighCpuRca;

@Category(GradleTaskForRca.class)
public class GenericResourceRcaTest {

    @Test
    public void testHighTotalCpuRca() {
        List<String> columnName =
                Arrays.asList(AllMetrics.CommonDimension.OPERATION.toString(), MetricsDB.AVG);
        MetricTestHelper cpuUtilizationGroupByOperation = new MetricTestHelper(5);
        // threshold is 0.7, lower bound is 0.7*0.5 = 0.35
        HighCpuRcaX highTotalCpuRcaX = new HighCpuRcaX(1, cpuUtilizationGroupByOperation);
        highTotalCpuRcaX.setThreshold(0.7);
        highTotalCpuRcaX.setLowerBoundThreshold(0.35);

        ResourceFlowUnit flowUnit;
        Clock constantClock = Clock.fixed(ofEpochMilli(0), ZoneId.systemDefault());

        // ts = 0, cpu = [0.2]
        cpuUtilizationGroupByOperation.createTestFlowUnits(
                columnName, Arrays.asList("App1", "0.2"));
        highTotalCpuRcaX.setClock(constantClock);
        flowUnit = highTotalCpuRcaX.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        // ts = 3, cpu = [0.2, 0.6]
        // above lower bound, start to send summary
        cpuUtilizationGroupByOperation.createTestFlowUnits(
                columnName, Arrays.asList("App1", "0.6"));
        highTotalCpuRcaX.setClock(Clock.offset(constantClock, Duration.ofMinutes(3)));
        flowUnit = highTotalCpuRcaX.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());
        Assert.assertTrue(flowUnit.hasResourceSummary());

        // ts = 11, cpu = [0.6, 0.8]
        // above lower bound, start to send summary
        cpuUtilizationGroupByOperation.createTestFlowUnits(
                columnName, Arrays.asList("App1", "0.8"));
        highTotalCpuRcaX.setClock(Clock.offset(constantClock, Duration.ofMinutes(11)));
        flowUnit = highTotalCpuRcaX.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        Assert.assertTrue(flowUnit.hasResourceSummary());

        // ts = 15, cpu = [0.8, 0.2]
        // above lower bound, start to send summary
        cpuUtilizationGroupByOperation.createTestFlowUnits(
                columnName, Arrays.asList("App1", "0.2"));
        highTotalCpuRcaX.setClock(Clock.offset(constantClock, Duration.ofMinutes(11)));
        flowUnit = highTotalCpuRcaX.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());
        Assert.assertTrue(flowUnit.hasResourceSummary());
    }

    private static class HighCpuRcaX extends HighCpuRca {
        public <M extends Metric> HighCpuRcaX(
                final int rcaPeriod, final M cpuUtilizationGroupByOperation) {
            super(rcaPeriod, cpuUtilizationGroupByOperation);
        }

        public void setClock(Clock testClock) {
            this.clock = testClock;
        }
    }
}
