/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.jvmsizing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.MetricTestHelper;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;

public class HighOldGenOccupancyRcaTest {

    // TODO: Extract the common pieces between this test and the old gen reclamation rca test.
    private static final long HEAP_UTILIZATION_THRESHOLD = 75L;
    private static final long PERIOD = 5L;
    private static final String CMS_COLLECTOR = "ConcurrentMarkSweep";
    private final List<String> heapTableColumns =
            Arrays.asList(
                    AllMetrics.HeapDimension.MEM_TYPE.toString(),
                    MetricsDB.SUM,
                    MetricsDB.AVG,
                    MetricsDB.MIN,
                    MetricsDB.MAX);

    @Mock private Metric mockHeapUsed;

    @Mock private Metric mockHeapMax;

    @Mock private Metric mockGcType;

    private HighOldGenOccupancyRca testRca;
    private MetricTestHelper metricTestHelper;

    @Before
    public void setup() {
        initMocks(this);
        this.metricTestHelper = new MetricTestHelper(PERIOD);
        setupMockHeapMetric(mockHeapUsed, 80.0);
        setupMockHeapMetric(mockHeapMax, 100.0);
        setupMockGcType(CMS_COLLECTOR);
        testRca =
                new HighOldGenOccupancyRca(
                        mockHeapMax, mockHeapUsed, mockGcType, HEAP_UTILIZATION_THRESHOLD, PERIOD);
    }

    @Test
    public void testHighOldGenOccupancyRca() {
        final ResourceFlowUnit<HotResourceSummary> flowUnit = testRca.operate();

        assertFalse(flowUnit.isEmpty());
        ResourceContext context = flowUnit.getResourceContext();
        assertTrue(context.isUnhealthy());
    }

    @Test
    public void testInvalidMaxHeap() {
        setupMockHeapMetric(mockHeapMax, 0);
        final ResourceFlowUnit<HotResourceSummary> flowUnit = testRca.operate();

        assertFalse(flowUnit.isEmpty());
        assertTrue(flowUnit.getResourceContext().isHealthy());
    }

    @Test
    public void testHeapHealthy() {
        setupMockHeapMetric(mockHeapUsed, 30.0);
        final ResourceFlowUnit<HotResourceSummary> flowUnit = testRca.operate();

        assertFalse(flowUnit.isEmpty());
        assertTrue(flowUnit.getResourceContext().isHealthy());
    }

    @Test
    public void testNonCMSGarbageCollector() {
        setupMockGcType("G1");
        final ResourceFlowUnit<HotResourceSummary> flowUnit = testRca.operate();

        assertTrue(flowUnit.isEmpty());
    }

    private void setupMockHeapMetric(final Metric metric, final double val) {
        String valString = Double.toString(val);
        List<String> data =
                Arrays.asList(
                        AllMetrics.GCType.OLD_GEN.toString(),
                        valString,
                        valString,
                        valString,
                        valString);
        when(metric.getFlowUnits())
                .thenReturn(
                        Collections.singletonList(
                                new MetricFlowUnit(
                                        0,
                                        metricTestHelper.createTestResult(
                                                heapTableColumns, data))));
    }

    private void setupMockGcType(final String collector) {
        List<String> gcInfoTableColumns =
                Arrays.asList(
                        AllMetrics.GCInfoDimension.MEMORY_POOL.toString(),
                        AllMetrics.GCInfoDimension.COLLECTOR_NAME.toString());
        List<String> data = Arrays.asList(AllMetrics.GCType.OLD_GEN.toString(), collector);
        when(mockGcType.getFlowUnits())
                .thenReturn(
                        Collections.singletonList(
                                new MetricFlowUnit(
                                        0,
                                        metricTestHelper.createTestResult(
                                                gcInfoTableColumns, data))));
    }
}
