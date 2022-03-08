/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.store.rca.hotheap;

import static java.time.Instant.ofEpochMilli;

import com.google.common.collect.Lists;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.GradleTaskForRca;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.MetricTestHelper;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.store.rca.hotheap.HighHeapUsageYoungGenRca;

@Category(GradleTaskForRca.class)
public class HighHeapUsageYoungGenRcaTest {
    private static final double CONVERT_MEGABYTES_TO_BYTES = Math.pow(1024, 2);
    private static final String CMS_COLLECTOR = "ConcurrentMarkSweep";
    private MetricTestHelper heap_Used;
    private MetricTestHelper gc_Collection_Time;
    private MetricTestHelper gc_Collection_Event;
    private MetricTestHelper gc_Type;
    private HighHeapUsageYoungGenRcaX youngGenRcaX;
    private List<String> columnName;

    /** generates mock metric flow units that RCAs can consume */
    private void mockFlowUnits(
            double heapUsageVal, double minorGcTime, double fullGcTime, double fullGcEvents) {
        heap_Used.createTestFlowUnits(
                columnName,
                Arrays.asList(
                        AllMetrics.GCType.OLD_GEN.toString(),
                        String.valueOf(heapUsageVal * CONVERT_MEGABYTES_TO_BYTES)));
        List<String> youngGcRow =
                Arrays.asList(
                        AllMetrics.GCType.TOT_YOUNG_GC.toString(), String.valueOf(minorGcTime));
        List<String> fullGcRow =
                Arrays.asList(AllMetrics.GCType.TOT_FULL_GC.toString(), String.valueOf(fullGcTime));
        gc_Collection_Time.createTestFlowUnitsWithMultipleRows(
                columnName, Lists.newArrayList(youngGcRow, fullGcRow));
        gc_Collection_Event.createTestFlowUnits(
                columnName,
                Arrays.asList(AllMetrics.GCType.OLD_GEN.toString(), String.valueOf(fullGcEvents)));
        gc_Type.createTestFlowUnits(
                Arrays.asList(
                        AllMetrics.GCInfoDimension.MEMORY_POOL.toString(),
                        AllMetrics.GCInfoDimension.COLLECTOR_NAME.toString()),
                Arrays.asList(AllMetrics.GCType.OLD_GEN.toString(), CMS_COLLECTOR));
    }

    @Before
    public void initTestHighHeapYoungGenRca() {
        heap_Used = new MetricTestHelper(5);
        gc_Collection_Time = new MetricTestHelper(5);
        gc_Collection_Event = new MetricTestHelper(5);
        gc_Type = new MetricTestHelper(5);
        youngGenRcaX =
                new HighHeapUsageYoungGenRcaX(
                        1, heap_Used, gc_Collection_Time, gc_Collection_Event, gc_Type);
        columnName = Arrays.asList(AllMetrics.HeapDimension.MEM_TYPE.toString(), MetricsDB.MAX);
    }

    @Test
    public void testHighHeapYoungGenRca() {
        ResourceFlowUnit flowUnit;
        Clock constantClock = Clock.fixed(ofEpochMilli(0), ZoneId.systemDefault());

        // ts = 0, heap = 0, minor gc time = 0, full gc time = 0ms
        mockFlowUnits(0, 0, 0, 0);
        youngGenRcaX.setClock(constantClock);
        flowUnit = youngGenRcaX.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        // ts = 1, heap = 450MB, minor gc time = 200ms, full gc time = 9000ms
        mockFlowUnits(450, 200, 9000, 0);
        youngGenRcaX.setClock(Clock.offset(constantClock, Duration.ofSeconds(1)));
        flowUnit = youngGenRcaX.operate();
        Assert.assertFalse(flowUnit.getResourceContext().isUnhealthy());

        // ts = 2, heap = 1050MB, minor gc time = 240ms, full gc time = 12000ms
        // the average full GC time is now 10.5s which is > the threshold of 10s
        mockFlowUnits(1050, 400, 24000, 0);
        youngGenRcaX.setClock(Clock.offset(constantClock, Duration.ofSeconds(2)));
        flowUnit = youngGenRcaX.operate();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        HotResourceSummary summary = (HotResourceSummary) flowUnit.getSummary();
        Assert.assertEquals(ResourceUtil.FULL_GC_PAUSE_TIME, summary.getResource());
        Assert.assertEquals(11000, summary.getValue(), 0.1);

        // ts = 3, heap = 1650MB, minor gc time = 600ms, full gc time = 0ms
        // the average promotion rate is now 550 MB/s which is > the threshold of 500 MB/s
        mockFlowUnits(1650, 600, 0, 0);
        youngGenRcaX.setClock(Clock.offset(constantClock, Duration.ofSeconds(3)));
        flowUnit = youngGenRcaX.operate();
        summary = (HotResourceSummary) flowUnit.getSummary();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        Assert.assertEquals(ResourceUtil.YOUNG_GEN_PROMOTION_RATE, summary.getResource());
        Assert.assertEquals(550, summary.getValue(), 0.1);

        // ts = 4, heap = 1650MB, minor gc time = 800ms, full gc time = 0ms
        // the average minor gc time is now 500 ms / sec which is > the threshold of 400 ms / sec
        mockFlowUnits(1650, 800, 0, 0);
        youngGenRcaX.setClock(Clock.offset(constantClock, Duration.ofSeconds(4)));
        flowUnit = youngGenRcaX.operate();
        summary = (HotResourceSummary) flowUnit.getSummary();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        Assert.assertEquals(ResourceUtil.MINOR_GC_PAUSE_TIME, summary.getResource());
        Assert.assertEquals(500, summary.getValue(), 0.1);

        // ts = 5, heap = 0MB, minor gc time = 0ms, full gc time = 0ms
        // the average garbage promotion percent is now 100% which is > the threshold of 80%
        mockFlowUnits(0, 0, 0, 1);
        youngGenRcaX.setClock(Clock.offset(constantClock, Duration.ofSeconds(5)));
        flowUnit = youngGenRcaX.operate();
        summary = (HotResourceSummary) flowUnit.getSummary();
        Assert.assertTrue(flowUnit.getResourceContext().isUnhealthy());
        Assert.assertEquals(ResourceUtil.YOUNG_GEN_PROMOTION_RATE, summary.getResource());
        Assert.assertEquals(1, summary.getValue(), 0.1);
    }

    private static class HighHeapUsageYoungGenRcaX extends HighHeapUsageYoungGenRca {
        public <M extends Metric> HighHeapUsageYoungGenRcaX(
                final int rcaPeriod,
                final M heap_Used,
                final M gc_Collection_Time,
                final M gc_Collection_Event,
                final M gc_Type) {
            super(rcaPeriod, heap_Used, gc_Collection_Time, gc_Collection_Event, gc_Type);
        }

        public void setClock(Clock testClock) {
            this.clock = testClock;
        }
    }
}
