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
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.MetricTestHelper;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;

public class OldGenReclamationRcaTest {

    // TODO: Extract the common pieces between this test and the high old gen occupancy rca test.
    private static final long HEAP_UTILIZATION_THRESHOLD = 75L;
    private static final int PERIOD = 5;
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

    @Mock private Metric mockGcEvents;

    @Mock private Metric mockGcType;

    private OldGenReclamationRca testRca;
    private MetricTestHelper metricTestHelper;

    @Before
    public void setup() {
        initMocks(this);
        this.metricTestHelper = new MetricTestHelper(PERIOD);
        this.testRca =
                new OldGenReclamationRca(
                        mockHeapUsed,
                        mockHeapMax,
                        mockGcEvents,
                        mockGcType,
                        HEAP_UTILIZATION_THRESHOLD,
                        PERIOD);
        setupMockHeapMetric(mockHeapUsed, 80.0);
        setupMockHeapMetric(mockHeapMax, 100.0);
        setupMockHeapMetric(mockGcEvents, AllMetrics.GCType.TOT_FULL_GC.toString(), 10.0);
        setupMockGcType(CMS_COLLECTOR);
    }

    @Test
    public void testOldGenReclamationRca() {
        final ResourceFlowUnit<HotResourceSummary> flowUnit = testRca.operate();
        assertFalse(flowUnit.isEmpty());

        assertTrue(flowUnit.getResourceContext().isUnhealthy());
    }

    @Test
    public void testHealthyReclamation() {
        setupMockHeapMetric(mockHeapUsed, 60.0);

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

    @Test
    public void testNoGcEvents() {
        setupMockHeapMetric(mockGcEvents, AllMetrics.GCType.TOT_FULL_GC.toString(), 0.0);
        final ResourceFlowUnit<HotResourceSummary> flowUnit = testRca.operate();

        assertFalse(flowUnit.isEmpty());
        assertTrue(flowUnit.getResourceContext().isUnknown());
    }

    private void setupMockHeapMetric(final Metric metric, final double val) {
        setupMockHeapMetric(metric, AllMetrics.GCType.OLD_GEN.toString(), val);
    }

    private void setupMockHeapMetric(
            final Metric metric, final String fieldName, final double val) {
        String valString = Double.toString(val);
        List<String> data = Arrays.asList(fieldName, valString, valString, valString, valString);
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
