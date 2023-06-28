/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.searchbackpressure;

import static org.opensearch.performanceanalyzer.rca.framework.api.persist.SQLParsingUtil.readDataFromSqlResult;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.configs.SearchBackPressureRcaConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.SlidingWindow;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.SlidingWindowData;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;
import org.opensearch.performanceanalyzer.rca.store.rca.OldGenRca;
import org.opensearch.performanceanalyzer.rca.store.rca.searchbackpressure.model.SearchBackPressureRCAMetric;

public class SearchBackPressureRCA extends OldGenRca<ResourceFlowUnit<HotNodeSummary>> {
    // LOGGER for SearchBackPressureRCA
    private static final Logger LOG = LogManager.getLogger(SearchBackPressureRCA.class);
    private static final double BYTES_TO_GIGABYTES = Math.pow(1024, 3);
    private static final long EVAL_INTERVAL_IN_S = 5;

    // Key Metrics to be used to determine health status
    private final Metric heapUsed;
    private final Metric searchbp_Stats;

    /*
     * Threshold
     * SearchBackPressureRCA Use heap usage and shard/task level searchbp cancellation count as threshold
     */
    private long SearchBPCancellationJVMThreshold;

    // threshold to increase heap limits
    private long heapUsedIncreaseMaxThreshold;
    private long heapCancellationIncreaseMaxThreshold;

    // threshold to decrease heap limits
    private long heapUsedDecreaseMinThreshold;
    private long heapCancellationDecreaseMaxThreashold;

    /*
     * Sliding Window
     * SearchBackPressureRCA keep track the continous performance of 3 key metrics
     * TaskJVMCancellationPercent/ShardJVMCancellationPercent/HeapUsagePercent
     */
    private final SlidingWindow<SlidingWindowData> taskJVMCancellationSlidingWindow;
    private final SlidingWindow<SlidingWindowData> shardJVMCancellationSlidingWindow;
    private final SlidingWindow<SlidingWindowData> heapUsageSlidingWindow;

    // Sliding Window Interval
    private static final int SLIDING_WINDOW_SIZE_IN_MINS = 1;
    private static final int SLIDING_WINDOW_SIZE_IN_SECS = SLIDING_WINDOW_SIZE_IN_MINS * 60;

    // counter to check the samples has been taken, only emit flow units when counter equals to
    // rcaPeriod
    private long counter;

    // Required amount of RCA period this RCA needs to run before sending out a flowunit
    private final int rcaPeriod;

    // Current time
    protected Clock clock;

    // key functions to be overriden
    // operate(): determine whether to generate of flow unit of HEALTHY or UNHEALTHY
    // readRcaConf(): read the key configuration metrics like heapMaxThreshold,
    // heapMinThreshold,
    // cancellationHeapPercentageThreshold
    // counter to keep track of times of checking, as the default sliding window is
    // 60 times, and
    // interval for RCA scanning is 5s
    // counter needs to be at least 12 to trigger operate(): 12 is the
    // rcaSamplesBeforeEval

    // generateFlowUnitListFromWite() gets wireFlowUnits() (Do we need this?)

    // Not to be overriden but need to have
    // read_cancellationcount_from_sql_shard
    // read_cancellationcount_from_sql_task
    // read_heapused_from_sql
    // for heapused, simply call getOldGenUsedOrDefault() from OldGenRca.java
    public <M extends Metric> SearchBackPressureRCA(
            final int rcaPeriod, final M heapMax, final M heapUsed, M gcType, M searchbp_Stats) {
        super(EVAL_INTERVAL_IN_S, heapUsed, heapMax, null, gcType);
        this.heapUsed = heapUsed;
        this.rcaPeriod = rcaPeriod;
        this.clock = Clock.systemUTC();
        this.searchbp_Stats = searchbp_Stats;
        this.heapUsedIncreaseMaxThreshold =
                SearchBackPressureRcaConfig.DEFAULT_MAX_HEAP_DOWNFLOW_THRESHOLD;
        this.heapCancellationIncreaseMaxThreshold =
                SearchBackPressureRcaConfig.DEFAULT_MAX_HEAP_CANCELLATION_THRESHOLD;
        this.heapUsedDecreaseMinThreshold =
                SearchBackPressureRcaConfig.DEFAULT_MIN_HEAP_OVERFLOW_THRESHOLD;
        this.heapCancellationDecreaseMaxThreashold =
                SearchBackPressureRcaConfig.DEFAULT_MIN_HEAP_CANCELLATION_THRESHOLD;

        // initialize sliding window
        this.heapUsageSlidingWindow =
                new SlidingWindow<>(SLIDING_WINDOW_SIZE_IN_MINS, TimeUnit.MINUTES);
        this.shardJVMCancellationSlidingWindow =
                new SlidingWindow<>(SLIDING_WINDOW_SIZE_IN_MINS, TimeUnit.MINUTES);
        this.taskJVMCancellationSlidingWindow =
                new SlidingWindow<>(SLIDING_WINDOW_SIZE_IN_MINS, TimeUnit.MINUTES);

        LOG.info("SearchBackPressureRCA initialized");
    }

    /*
     * operate() is used for local build
     * generateFlowUnitListFromWire simply use remote flowunits to
     */
    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        final List<FlowUnitMessage> flowUnitMessages =
                args.getWireHopper().readFromWire(args.getNode());
        final List<ResourceFlowUnit<HotNodeSummary>> flowUnitList = new ArrayList<>();
        LOG.debug("rca: Executing fromWire: {}", this.getClass().getSimpleName());
        for (FlowUnitMessage flowUnitMessage : flowUnitMessages) {
            flowUnitList.add(ResourceFlowUnit.buildFlowUnitFromWrapper(flowUnitMessage));
        }
        setFlowUnits(flowUnitList);
    }

    @Override
    public ResourceFlowUnit<HotNodeSummary> operate() {
        LOG.info("SearchBackPressureRCA operate() intiatilized");
        counter += 1;

        long currTimeStamp = this.clock.millis();

        // read key metrics into searchBackPressureRCAMetric for easier management
        SearchBackPressureRCAMetric searchBackPressureRCAMetric = getSearchBackPressureRCAMetric();

        // print out oldGenUsed and maxOldGen
        LOG.info(
                "SearchBackPressureRCA: oldGenUsed: {} maxOldGen: {}, heapUsedPercentage: {}, searchbpShardCancellationCount: {}, searchbpTaskCancellationCount: {}, searchbpJVMShardCancellationCount: {}, searchbpJVMTaskCancellationCount: {}",
                searchBackPressureRCAMetric.getUsedHeap(),
                searchBackPressureRCAMetric.getMaxHeap(),
                searchBackPressureRCAMetric.getHeapUsagePercent(),
                searchBackPressureRCAMetric.getSearchbpShardCancellationCount(),
                searchBackPressureRCAMetric.getSearchbpTaskCancellationCount(),
                searchBackPressureRCAMetric.getSearchbpJVMShardCancellationCount(),
                searchBackPressureRCAMetric.getSearchbpJVMTaskCancellationCount());

        // update sliding window if the value is NOT NaN
        double prevheapUsagePercentage = searchBackPressureRCAMetric.getHeapUsagePercent();
        if (!Double.isNaN(prevheapUsagePercentage)) {
            heapUsageSlidingWindow.next(
                    new SlidingWindowData(currTimeStamp, prevheapUsagePercentage));
        }

        double shardJVMCancellationPercentage =
                searchBackPressureRCAMetric.getShardJVMCancellationPercent();
        if (!Double.isNaN(shardJVMCancellationPercentage)) {
            shardJVMCancellationSlidingWindow.next(
                    new SlidingWindowData(currTimeStamp, shardJVMCancellationPercentage));
        }

        double taskJVMCancellationPercentage =
                searchBackPressureRCAMetric.getTaskJVMCancellationPercent();
        if (!Double.isNaN(taskJVMCancellationPercentage)) {
            taskJVMCancellationSlidingWindow.next(
                    new SlidingWindowData(currTimeStamp, taskJVMCancellationPercentage));
        }

        LOG.info("SearchBackPressureRCA counter is {}", counter);
        // if counter matches the rca period, emit the flow unit
        if (counter == this.rcaPeriod) {
            ResourceContext context = null;
            LOG.info("SearchBackPressureRCA counter in rcaPeriod is {}", counter);
            counter = 0;

            // TODO change to 
            double maxHeapUsagePercentage = heapUsageSlidingWindow.readAvg();
            double avgShardJVMCancellationPercentage = shardJVMCancellationSlidingWindow.readAvg();
            double avgTaskJVMCancellationPercentage = taskJVMCancellationSlidingWindow.readAvg();
            LOG.info(
                    "SearchBackPressureRCA: maxHeapUsagePercentage: {}, SearchBackPressureRCA: maxHeapUsagePercentage: {}, SearchBackPressureRCA: maxHeapUsagePercentage: {}",
                    maxHeapUsagePercentage,
                    avgShardJVMCancellationPercentage,
                    avgTaskJVMCancellationPercentage);

            // get the Configured Threshold and compare with Sliding Window Stats
            if (maxHeapUsagePercentage > heapUsedDecreaseMinThreshold) {
                // Generate a flow unit with an Unhealthy ResourceContext
                LOG.info(
                        "maxHeapUsagePercentage: {} is greater than threshold: {}",
                        maxHeapUsagePercentage,
                        heapUsedDecreaseMinThreshold);

            } else {
                // Generate a flow unit with a Healthy ResourceContext
                LOG.info(
                        "maxHeapUsagePercentage: {} is less than threshold: {}",
                        maxHeapUsagePercentage,
                        heapUsedDecreaseMinThreshold);
            }

        } else {
            LOG.info("Empty FlowUnit returned for High Heap Usage RCA");
            return new ResourceFlowUnit<>(this.clock.millis());
        }

        LOG.info("SearchBackPressureRCA operate() finished");
        return null;
    }

    private SearchBackPressureRCAMetric getSearchBackPressureRCAMetric() {
        // Get Heap Usage related metrics
        double prevHeapUsage = getOldGenUsedOrDefault(0d);
        double maxHeapSize = getMaxOldGenSizeOrDefault(Double.MAX_VALUE);

        // Get SearchBack Pressure related metrics from stats type field
        Field<String> searchbp_stats_type_field =
                DSL.field(
                        DSL.name(
                                AllMetrics.SearchBackPressureStatsValue.SEARCHBP_TYPE_DIM
                                        .toString()),
                        String.class);

        double searchbpShardCancellationCount =
                getMetric(
                        this.searchbp_Stats,
                        searchbp_stats_type_field,
                        AllMetrics.SearchBackPressureStatsValue
                                .SEARCHBP_SHARD_STATS_CANCELLATIONCOUNT
                                .toString());
        double searchbpTaskCancellationCount =
                getMetric(
                        this.searchbp_Stats,
                        searchbp_stats_type_field,
                        AllMetrics.SearchBackPressureStatsValue
                                .SEARCHBP_TASK_STATS_CANCELLATIONCOUNT
                                .toString());
        double searchbpJVMShardCancellationCount =
                getMetric(
                        this.searchbp_Stats,
                        searchbp_stats_type_field,
                        AllMetrics.SearchBackPressureStatsValue
                                .SEARCHBP_SHARD_STATS_RESOURCE_HEAP_USAGE_CANCELLATIONCOUNT
                                .toString());
        double searchbpJVMTaskCancellationCount =
                getMetric(
                        this.searchbp_Stats,
                        searchbp_stats_type_field,
                        AllMetrics.SearchBackPressureStatsValue
                                .SEARCHBP_TASK_STATS_RESOURCE_HEAP_USAGE_CANCELLATIONCOUNT
                                .toString());

        return new SearchBackPressureRCAMetric(
                prevHeapUsage,
                maxHeapSize,
                searchbpShardCancellationCount,
                searchbpTaskCancellationCount,
                searchbpJVMShardCancellationCount,
                searchbpJVMTaskCancellationCount);
    }

    private <M extends Metric> double getMetric(M metric, Field<String> field, String fieldName) {
        double response = 0;
        for (MetricFlowUnit flowUnit : metric.getFlowUnits()) {
            if (!flowUnit.isEmpty()) {
                double metricResponse =
                        readDataFromSqlResult(flowUnit.getData(), field, fieldName, MetricsDB.MAX);
                if (!Double.isNaN(metricResponse) && metricResponse > 0) {
                    response = metricResponse;
                }
            }
        }
        return response;
    }

    /**
     * read threshold values from rca.conf
     *
     * @param conf RcaConf object
     */
    @Override
    public void readRcaConf(RcaConf conf) {
        // only initialized one time
        LOG.info("SearchBackPressureRCA readRcaConf() intiatilized");
        final SearchBackPressureRcaConfig config = conf.getSearchBackPressureRcaConfig();
        // read anything from config file in runtime
        // if not just skip it
    }
}
