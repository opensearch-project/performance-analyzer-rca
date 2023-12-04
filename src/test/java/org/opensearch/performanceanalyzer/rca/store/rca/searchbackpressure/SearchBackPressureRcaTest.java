/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.searchbackpressure;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil.SEARCHBACKPRESSURE_SHARD;
import static org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil.SEARCHBACKPRESSURE_TASK;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.configs.SearchBackPressureRcaConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.MetricTestHelper;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;

public class SearchBackPressureRcaTest {
    // Mock Metrics
    @Mock private Metric mockHeapUsed;

    @Mock private Metric mockHeapMax;

    @Mock private Metric mockGcType;

    @Mock private Metric mockSearchbpStats;

    // every 5s operate() gets initiated
    private static final int RCA_PERIOD = 5;

    private SearchBackPressureRCA testRca;
    private MetricTestHelper metricTestHelper;
    private static final double DEFAULT_MAX_HEAP_SIZE = 4294967296.0;

    // mock heap metric columns
    private final List<String> heapTableColumns =
            Arrays.asList(
                    AllMetrics.HeapDimension.MEM_TYPE.toString(),
                    MetricsDB.SUM,
                    MetricsDB.AVG,
                    MetricsDB.MIN,
                    MetricsDB.MAX);

    // mock search back pressure metric columns
    private final List<String> searchbpTableColumns =
            Arrays.asList(
                    AllMetrics.SearchBackPressureStatsValue.SEARCHBP_TYPE_DIM.toString(),
                    MetricsDB.SUM,
                    MetricsDB.AVG,
                    MetricsDB.MIN,
                    MetricsDB.MAX);

    // dummy field to create a mock gcType Metric
    private static final String CMS_COLLECTOR = "ConcurrentMarkSweep";

    /*
     * initialization before running any test
     *
     */
    @Before
    public void setup() throws Exception {
        initMocks(this);
        this.metricTestHelper = new MetricTestHelper(RCA_PERIOD);
        setupMockHeapMetric(mockHeapUsed, 80.0);
        setupMockHeapMetric(mockHeapMax, 100.0);

        // set up SearchBp_Stats table
        setupMockSearchbpStats(mockSearchbpStats, 10.0, 10.0, 200, 100, 8.0, 7.0);

        this.testRca =
                new SearchBackPressureRCA(RCA_PERIOD, mockHeapMax, mockHeapUsed, mockSearchbpStats);
    }

    /*
     * Test SearchBackPressure RCA returns empty resourceFlowUnit if counter is less than the rcaPeriod
     */
    @Test
    public void testSearchBpGetResourceContextLessRcaPeriod() {
        setupMockHeapMetric(mockHeapMax, DEFAULT_MAX_HEAP_SIZE);
        setupMockHeapMetric(mockHeapUsed, DEFAULT_MAX_HEAP_SIZE * 0.8);
        setupMockSearchbpStats(mockSearchbpStats, 10.0, 10.0, 200, 100, 8.0, 7.0);

        ResourceFlowUnit<HotNodeSummary> flowUnit = testRca.operate();

        // counter = 1
        // counter needs to equal to RcaPeriod (5 in this case) to get nonempty resourceflowunit
        assertTrue(flowUnit.isEmpty());
    }

    /*
     * Test SearchBackPressure RCA returns nonempty resourceFlowUnit if counter equals to rcaPeriod
     */
    @Test
    public void testSearchBpGetResourceContextEqualRcaPeriod() {
        setupMockHeapMetric(mockHeapMax, DEFAULT_MAX_HEAP_SIZE);
        setupMockHeapMetric(mockHeapUsed, DEFAULT_MAX_HEAP_SIZE * 0.8);
        setupMockSearchbpStats(mockSearchbpStats, 10.0, 10.0, 200, 100, 8.0, 7.0);
        IntStream.range(0, RCA_PERIOD - 1).forEach(i -> testRca.operate());

        ResourceFlowUnit<HotNodeSummary> flowUnit = testRca.operate();

        // counter = RCA_PERIOD
        // counter needs to equal to RcaPeriod (5 in this case) to get nonempty resourceflowunit
        assertFalse(flowUnit.isEmpty());
    }

    /*
     * Test SearchBackPressure RCA returns healthy nonempty flow units if the settings does not trigger autotune
     * Meeting None of Increasing or Decreasing Threshold for both shard/task level
     */
    @Test
    public void testSearchBpGetHealthyFlowUnit() {
        setupMockHeapMetric(mockHeapMax, DEFAULT_MAX_HEAP_SIZE);
        setupMockHeapMetric(mockHeapUsed, DEFAULT_MAX_HEAP_SIZE * 0.8);
        setupMockSearchbpStats(mockSearchbpStats, 10.0, 10.0, 200, 100, 8.0, 7.0);
        IntStream.range(0, RCA_PERIOD - 1).forEach(i -> testRca.operate());

        ResourceFlowUnit<HotNodeSummary> flowUnit = testRca.operate();
        assertFalse(flowUnit.isEmpty());
        assertTrue(flowUnit.getResourceContext().isHealthy());
    }

    /*
     * Test SearchBackPressure RCA returns unhealthy nonempty flow units if the settings does trigger autotune by increasing threshold
     * Increasing threshold:
     * node max heap usage in last 60 secs is less than 70%
     * cancellationCount due to heap is more than 5% of all task cancellations (Shard-Level)
     */
    @Test
    public void testSearchBpGetUnHealthyFlowUnitByShardIncreaseThreshold() {
        setupMockHeapMetric(mockHeapMax, DEFAULT_MAX_HEAP_SIZE);
        setupMockHeapMetric(mockHeapUsed, DEFAULT_MAX_HEAP_SIZE * 0.3);
        setupMockSearchbpStats(mockSearchbpStats, 10.0, 10.0, 10, 10, 1.2, 4.0);
        IntStream.range(0, RCA_PERIOD - 1).forEach(i -> testRca.operate());

        ResourceFlowUnit<HotNodeSummary> flowUnit = testRca.operate();
        assertFalse(flowUnit.isEmpty());
        assertFalse(flowUnit.getResourceContext().isHealthy());
    }

    /*
     * Test SearchBackPressure RCA returns unhealthy nonempty flow units if the settings does trigger autotune by increasing threshold
     * Increasing threshold:
     * node max heap usage in last 60 secs is less than 70%
     * cancellationCount due to heap is more than 5% of all task completions (Task-Level).
     */
    @Test
    public void testSearchBpGetUnHealthyFlowUnitByTaskIncreaseThreshold() {
        setupMockHeapMetric(mockHeapMax, DEFAULT_MAX_HEAP_SIZE);
        setupMockHeapMetric(mockHeapUsed, DEFAULT_MAX_HEAP_SIZE * 0.3);
        setupMockSearchbpStats(mockSearchbpStats, 10.0, 10.0, 10, 10, 4.0, 8.0);
        IntStream.range(0, RCA_PERIOD - 1).forEach(i -> testRca.operate());

        ResourceFlowUnit<HotNodeSummary> flowUnit = testRca.operate();
        assertFalse(flowUnit.isEmpty());
        assertFalse(flowUnit.getResourceContext().isHealthy());
    }

    /*
     * Test SearchBackPressure RCA returns unhealthy nonempty flow units if the settings does trigger autotune by decreasing threshold
     * decreasing threshold:
     * node min heap usage in last 60 secs is more than 80%
     * cancellationCount due to heap is less than 30% of all task cancellations (Shard-Level)
     */
    @Test
    public void testSearchBpGetUnHealthyFlowUnitByShardDecreaseThreshold() {
        setupMockHeapMetric(mockHeapMax, DEFAULT_MAX_HEAP_SIZE);
        setupMockHeapMetric(mockHeapUsed, DEFAULT_MAX_HEAP_SIZE * 0.9);
        setupMockSearchbpStats(mockSearchbpStats, 10.0, 10.0, 10, 10, 0, 0);
        IntStream.range(0, RCA_PERIOD - 1).forEach(i -> testRca.operate());

        ResourceFlowUnit<HotNodeSummary> flowUnit = testRca.operate();
        assertFalse(flowUnit.isEmpty());
        assertFalse(flowUnit.getResourceContext().isHealthy());
    }

    /*
     * Test SearchBackPressure RCA returns unhealthy nonempty flow units if the settings does trigger autotune by decreasing threshold
     * decreasing threshold:
     * node min heap usage in last 60 secs is more than 80%
     * cancellationCount due to heap is less than 30% of all task completions (Task-Level)
     */
    @Test
    public void testSearchBpGetUnHealthyFlowUnitByTaskDecreaseThreshold() {
        setupMockHeapMetric(mockHeapMax, DEFAULT_MAX_HEAP_SIZE);
        setupMockHeapMetric(mockHeapUsed, DEFAULT_MAX_HEAP_SIZE * 0.9);
        setupMockSearchbpStats(mockSearchbpStats, 10.0, 10.0, 10, 10, 0, 0);
        IntStream.range(0, RCA_PERIOD - 1).forEach(i -> testRca.operate());

        ResourceFlowUnit<HotNodeSummary> flowUnit = testRca.operate();
        assertFalse(flowUnit.isEmpty());
        assertFalse(flowUnit.getResourceContext().isHealthy());
    }

    /*
     * Test SearchBackPressure RCA returns unhealthy nonempty flow units with a HotResourceSummary of SEARCHBACKPRESSURE_SHARD Resource
     * indicating the autotune (unhealthy resource unit) is caused by meeting the threshold in shard-level in decrease threshold
     * decreasing threshold:
     * node min heap usage in last 60 secs is more than 80%
     * cancellationCount due to heap is less than 3% of all task cancellations (Shard-Level)
     */
    @Test
    public void testSearchBpGetUnHealthyFlowUnitInShardLevelByDecreaseThreshold() {
        setupMockHeapMetric(mockHeapMax, DEFAULT_MAX_HEAP_SIZE);
        setupMockHeapMetric(mockHeapUsed, DEFAULT_MAX_HEAP_SIZE * 0.95);
        setupMockSearchbpStats(mockSearchbpStats, 10.0, 10.0, 10, 10, 0.1, 8.0);
        IntStream.range(0, RCA_PERIOD - 1).forEach(i -> testRca.operate());

        ResourceFlowUnit<HotNodeSummary> flowUnit = testRca.operate();
        assertFalse(flowUnit.isEmpty());
        assertFalse(flowUnit.getResourceContext().isHealthy());

        HotNodeSummary hotNodeSummary = flowUnit.getSummary();
        List<HotResourceSummary> hotResourceSummaries = hotNodeSummary.getHotResourceSummaryList();
        boolean found_shard_resource =
                hotResourceSummaries.stream()
                        .anyMatch(
                                hotResourceSummary ->
                                        (hotResourceSummary.getResource()
                                                        == SEARCHBACKPRESSURE_SHARD)
                                                && (hotResourceSummary.getMetaData()
                                                        == SearchBackPressureRcaConfig
                                                                .DECREASE_THRESHOLD_BY_JVM_STR));

        assertTrue(found_shard_resource);
    }

    /*
     * Test SearchBackPressure RCA returns unhealthy nonempty flow units with a HotResourceSummary of SEARCHBACKPRESSURE_SHARD Resource
     * indicating the autotune (unhealthy resource unit) is caused by meeting the threshold in shard-level in increase threshold
     * Increasing threshold:
     * node max heap usage in last 60 secs is less than 70%
     * cancellationCount due to heap is more than 50% of all task cancellations (Shard-Level)
     */
    @Test
    public void testSearchBpGetUnHealthyFlowUnitInShardLevelByIncreaseThreshold() {
        setupMockHeapMetric(mockHeapMax, DEFAULT_MAX_HEAP_SIZE);
        setupMockHeapMetric(mockHeapUsed, DEFAULT_MAX_HEAP_SIZE * 0.5);
        setupMockSearchbpStats(mockSearchbpStats, 10.0, 10.0, 10, 10, 8.0, 2.0);
        IntStream.range(0, RCA_PERIOD - 1).forEach(i -> testRca.operate());

        ResourceFlowUnit<HotNodeSummary> flowUnit = testRca.operate();
        assertFalse(flowUnit.isEmpty());
        assertFalse(flowUnit.getResourceContext().isHealthy());

        HotNodeSummary hotNodeSummary = flowUnit.getSummary();
        List<HotResourceSummary> hotResourceSummaries = hotNodeSummary.getHotResourceSummaryList();
        boolean found_shard_resource_and_increase_metadata =
                hotResourceSummaries.stream()
                        .anyMatch(
                                hotResourceSummary ->
                                        (hotResourceSummary.getResource()
                                                        == SEARCHBACKPRESSURE_SHARD)
                                                && (hotResourceSummary.getMetaData()
                                                        == SearchBackPressureRcaConfig
                                                                .INCREASE_THRESHOLD_BY_JVM_STR));

        assertTrue(found_shard_resource_and_increase_metadata);
    }

    /*
     * Test SearchBackPressure RCA returns unhealthy nonempty flow units with a HotResourceSummary of SEARCHBACKPRESSURE_SHARD Resource
     * indicating the autotune (unhealthy resource unit) is caused by meeting the threshold in task-level
     * decreasing threshold:
     * node min heap usage in last 60 secs is more than 80%
     * cancellationCount due to heap is less than 3% of all task completions (Task-Level)
     */
    @Test
    public void testSearchBpGetUnHealthyFlowUnitInTaskLevelByDecreaseThreshold() {
        setupMockHeapMetric(mockHeapMax, DEFAULT_MAX_HEAP_SIZE);
        setupMockHeapMetric(mockHeapUsed, DEFAULT_MAX_HEAP_SIZE * 0.9);
        setupMockSearchbpStats(mockSearchbpStats, 10.0, 10.0, 10, 10, 1, 0.1);
        IntStream.range(0, RCA_PERIOD - 1).forEach(i -> testRca.operate());

        ResourceFlowUnit<HotNodeSummary> flowUnit = testRca.operate();
        assertFalse(flowUnit.isEmpty());
        assertFalse(flowUnit.getResourceContext().isHealthy());

        HotNodeSummary hotNodeSummary = flowUnit.getSummary();
        List<HotResourceSummary> hotResourceSummaries = hotNodeSummary.getHotResourceSummaryList();
        boolean found_task_resource =
                hotResourceSummaries.stream()
                        .anyMatch(
                                hotResourceSummary ->
                                        (hotResourceSummary.getResource()
                                                        == SEARCHBACKPRESSURE_TASK)
                                                && (hotResourceSummary.getMetaData()
                                                        == SearchBackPressureRcaConfig
                                                                .DECREASE_THRESHOLD_BY_JVM_STR));

        assertTrue(found_task_resource);
    }

    /*
     * Test SearchBackPressure RCA returns unhealthy nonempty flow units with a HotResourceSummary of SEARCHBACKPRESSURE_SHARD Resource
     * indicating the autotune (unhealthy resource unit) is caused by meeting the threshold in shard-level
     * Increasing threshold:
     * node max heap usage in last 60 secs is less than 70%
     * cancellationCount due to heap is more than 5% of all task completions (Task-Level)
     */
    @Test
    public void testSearchBpGetUnHealthyFlowUnitInTaskLevelByIncreaseThreshold() {
        setupMockHeapMetric(mockHeapMax, DEFAULT_MAX_HEAP_SIZE);
        setupMockHeapMetric(mockHeapUsed, DEFAULT_MAX_HEAP_SIZE * 0.5);
        setupMockSearchbpStats(mockSearchbpStats, 10.0, 10.0, 10, 10, 0, 0.6);
        IntStream.range(0, RCA_PERIOD - 1).forEach(i -> testRca.operate());

        ResourceFlowUnit<HotNodeSummary> flowUnit = testRca.operate();
        assertFalse(flowUnit.isEmpty());
        assertFalse(flowUnit.getResourceContext().isHealthy());

        HotNodeSummary hotNodeSummary = flowUnit.getSummary();
        List<HotResourceSummary> hotResourceSummaries = hotNodeSummary.getHotResourceSummaryList();
        boolean found_task_resource =
                hotResourceSummaries.stream()
                        .anyMatch(
                                hotResourceSummary ->
                                        (hotResourceSummary.getResource()
                                                        == SEARCHBACKPRESSURE_TASK)
                                                && (hotResourceSummary.getMetaData()
                                                        == SearchBackPressureRcaConfig
                                                                .INCREASE_THRESHOLD_BY_JVM_STR));

        assertTrue(found_task_resource);
    }

    private void setupMockHeapMetric(final Metric metric, final double val) {
        String valString = Double.toString(val);
        List<String> data =
                Arrays.asList(
                        AllMetrics.GCType.HEAP.toString(),
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

    private void setupMockSearchbpStats(
            final Metric metric,
            final double searchbpShardCancellationCount,
            final double searchbpTaskCancellationCount,
            final double searchShardTaskCompletionCount,
            final double searchTaskCompletionCount,
            final double searchbpJVMShardCancellationCount,
            final double searchbpJVMTaskCancellationCount) {
        String searchbpShardCancellationCountStr = Double.toString(searchbpShardCancellationCount);
        String searchbpTaskCancellationCountStr = Double.toString(searchbpTaskCancellationCount);
        String searchbpJVMShardCancellationCountStr =
                Double.toString(searchbpJVMShardCancellationCount);
        String searchbpJVMTaskCancellationCountStr =
                Double.toString(searchbpJVMTaskCancellationCount);
        String searchShardTaskCompletionCountStr = Double.toString(searchShardTaskCompletionCount);
        String searchTaskCompletionCountStr = Double.toString(searchTaskCompletionCount);

        // add searchbpShardCancellationCountStr row
        List<String> searchbpShardCancellationCountRow =
                Arrays.asList(
                        AllMetrics.SearchBackPressureStatsValue
                                .SEARCHBP_SHARD_TASK_STATS_CANCELLATION_COUNT
                                .toString(),
                        searchbpShardCancellationCountStr,
                        searchbpShardCancellationCountStr,
                        searchbpShardCancellationCountStr,
                        searchbpShardCancellationCountStr);

        // add searchbpTaskCancellationCountStr row
        List<String> searchbpTaskCancellationCountRow =
                Arrays.asList(
                        AllMetrics.SearchBackPressureStatsValue
                                .SEARCHBP_SEARCH_TASK_STATS_CANCELLATION_COUNT
                                .toString(),
                        searchbpTaskCancellationCountStr,
                        searchbpTaskCancellationCountStr,
                        searchbpTaskCancellationCountStr,
                        searchbpTaskCancellationCountStr);

        List<String> searchShardTaskCompletionCountRow =
                Arrays.asList(
                        AllMetrics.SearchBackPressureStatsValue
                                .SEARCHBP_SHARD_TASK_STATS_COMPLETION_COUNT
                                .toString(),
                        searchShardTaskCompletionCountStr,
                        searchShardTaskCompletionCountStr,
                        searchShardTaskCompletionCountStr,
                        searchShardTaskCompletionCountStr);

        List<String> searchTaskCompletionCountRow =
                Arrays.asList(
                        AllMetrics.SearchBackPressureStatsValue
                                .SEARCHBP_SEARCH_TASK_STATS_COMPLETION_COUNT
                                .toString(),
                        searchTaskCompletionCountStr,
                        searchTaskCompletionCountStr,
                        searchTaskCompletionCountStr,
                        searchTaskCompletionCountStr);

        // add searchbpJVMShardCancellationCountStr row
        List<String> searchbpJVMShardCancellationCountRow =
                Arrays.asList(
                        AllMetrics.SearchBackPressureStatsValue
                                .SEARCHBP_SHARD_TASK_STATS_RESOURCE_HEAP_USAGE_CANCELLATION_COUNT
                                .toString(),
                        searchbpJVMShardCancellationCountStr,
                        searchbpJVMShardCancellationCountStr,
                        searchbpJVMShardCancellationCountStr,
                        searchbpJVMShardCancellationCountStr);

        // add searchbpJVMTaskCancellationCountStr row
        List<String> searchbpJVMTaskCancellationCountRow =
                Arrays.asList(
                        AllMetrics.SearchBackPressureStatsValue
                                .SEARCHBP_SEARCH_TASK_STATS_RESOURCE_HEAP_USAGE_CANCELLATION_COUNT
                                .toString(),
                        searchbpJVMTaskCancellationCountStr,
                        searchbpJVMTaskCancellationCountStr,
                        searchbpJVMTaskCancellationCountStr,
                        searchbpJVMTaskCancellationCountStr);

        List<MetricFlowUnit> flowUnits =
                Arrays.asList(
                        new MetricFlowUnit(
                                0,
                                metricTestHelper.createTestResult(
                                        searchbpTableColumns, searchbpShardCancellationCountRow)),
                        new MetricFlowUnit(
                                0,
                                metricTestHelper.createTestResult(
                                        searchbpTableColumns, searchbpTaskCancellationCountRow)),
                        new MetricFlowUnit(
                                0,
                                metricTestHelper.createTestResult(
                                        searchbpTableColumns, searchShardTaskCompletionCountRow)),
                        new MetricFlowUnit(
                                0,
                                metricTestHelper.createTestResult(
                                        searchbpTableColumns, searchTaskCompletionCountRow)),
                        new MetricFlowUnit(
                                0,
                                metricTestHelper.createTestResult(
                                        searchbpTableColumns,
                                        searchbpJVMShardCancellationCountRow)),
                        new MetricFlowUnit(
                                0,
                                metricTestHelper.createTestResult(
                                        searchbpTableColumns,
                                        searchbpJVMTaskCancellationCountRow)));

        when(metric.getFlowUnits()).thenReturn(flowUnits);
    }
}
