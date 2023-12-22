/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.searchbackpressure;

import static org.opensearch.performanceanalyzer.LocalhostConnectionUtil.ClusterSettings.SETTING_NOT_FOUND;
import static org.opensearch.performanceanalyzer.rca.framework.api.persist.SQLParsingUtil.readDataFromSqlResult;
import static org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil.SEARCHBACKPRESSURE_SHARD;
import static org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil.SEARCHBACKPRESSURE_TASK;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.opensearch.performanceanalyzer.LocalhostConnectionUtil;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.configs.SearchBackPressureRcaConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.SlidingWindow;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.SlidingWindowData;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.persist.SQLParsingUtil;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;
import org.opensearch.performanceanalyzer.rca.store.rca.OldGenRca.MinMaxSlidingWindow;
import org.opensearch.performanceanalyzer.rca.store.rca.searchbackpressure.model.SearchBackPressureRCAMetric;

public class SearchBackPressureRCA extends Rca<ResourceFlowUnit<HotNodeSummary>> {
    private static final Logger LOG = LogManager.getLogger(SearchBackPressureRCA.class);
    private static final double BYTES_TO_GIGABYTES = Math.pow(1024, 3);
    private static final long EVAL_INTERVAL_IN_S = SearchBackPressureRcaConfig.EVAL_INTERVAL_IN_S;
    private static final String SEARCH_BACKPRESSURE_HEAP_DURESS_KEY =
            "search_backpressure.node_duress.heap_threshold";
    private static final String SEARCH_BACKPRESSURE_HEAP_DURESS_VAL_REGEX = "([0-9].[0-9]+)";
    private static final double CONVERT_BYTES_TO_MEGABYTES = Math.pow(1024, 2);
    public static final int MAX_ALLOWED_HEAP = 90;
    public static final double MAX_GAP_BW_BASELINE_HEAP_AND_MAX_ALLOWED = 0.1;

    // Key metrics used to determine RCA Flow Unit health status
    private final Metric heapUsed;
    private final Metric heapMax;
    private final Metric searchbp_Stats;

    // default value for heapUsed and heapMax
    private static final double DEFAULT_HEAP_VAL = 0.0;

    /*
     * threshold to increase heap limits
     */
    private long heapUsedIncreaseThreshold;
    // shard-level searchbp heap cancellation increase threshold
    private long heapShardCancellationIncreaseMaxThreshold;

    // task-level searchbp heap cancellation increase threshold
    private long heapTaskCancellationIncreaseMaxThreshold;

    /*
     * threshold to decrease heap limits
     */
    private long heapUsedDecreaseThreshold;
    // shard-level searchbp heap cancellation decrease threshold
    private long heapShardCancellationDecreaseMinThreashold;

    // task-level searchbp heap cancellation decrease threshold
    private long heapTaskCancellationDecreaseMinThreashold;

    /*
     * Sliding Window
     * SearchBackPressureRCA keep track the continous performance of 3 key metrics
     * TaskJVMCancellationPercent/ShardJVMCancellationPercent/HeapUsagePercent
     */
    private final SlidingWindow<SlidingWindowData> taskJVMCancellationSlidingWindow;
    private final SlidingWindow<SlidingWindowData> shardJVMCancellationSlidingWindow;
    private final MinMaxSlidingWindow minHeapUsageSlidingWindow;
    private final MinMaxSlidingWindow maxHeapUsageSlidingWindow;

    // Sliding Window Interval
    private static final int SLIDING_WINDOW_SIZE_IN_MINS =
            SearchBackPressureRcaConfig.SLIDING_WINDOW_SIZE_IN_MINS;
    private static final int SLIDING_WINDOW_SIZE_IN_SECS = SLIDING_WINDOW_SIZE_IN_MINS * 60;

    // currentIterationNumber to check the samples has been taken, only emit flow units when
    // currentIterationNumber equals to
    // rcaPeriod
    private long currentIterationNumber;

    // Required amount of RCA period this RCA needs to run before sending out a flowunit
    private final int rcaPeriod;

    // Current time
    protected Clock clock;

    // lambda function to add nextElement to monotonically decreasing sliding window
    BiConsumer<Deque<SlidingWindowData>, SlidingWindowData> minSlidingWindowNextElement =
            (windowDeque, nextElement) -> {
                while (!windowDeque.isEmpty()
                        && windowDeque.peekFirst().getValue() >= nextElement.getValue()) {
                    windowDeque.pollFirst();
                }

                windowDeque.addFirst(nextElement);
            };

    // lambda function to add nextElement to monotonically increasing sliding window
    BiConsumer<Deque<SlidingWindowData>, SlidingWindowData> maxSlidingWindowNextElement =
            (windowDeque, nextElement) -> {
                while (!windowDeque.isEmpty()
                        && windowDeque.peekFirst().getValue() < nextElement.getValue()) {
                    windowDeque.pollFirst();
                }

                windowDeque.addFirst(nextElement);
            };

    public <M extends Metric> SearchBackPressureRCA(
            final int rcaPeriod, final M heapMax, final M heapUsed, M searchbp_Stats) {
        super(EVAL_INTERVAL_IN_S);
        this.heapUsed = heapUsed;
        this.heapMax = heapMax;
        this.rcaPeriod = rcaPeriod;
        this.clock = Clock.systemUTC();
        this.searchbp_Stats = searchbp_Stats;

        // threshold for heap usage
        this.heapUsedIncreaseThreshold =
                SearchBackPressureRcaConfig.DEFAULT_MAX_HEAP_INCREASE_THRESHOLD_PERCENT;
        this.heapUsedDecreaseThreshold =
                SearchBackPressureRcaConfig.DEFAULT_MIN_HEAP_DECREASE_THRESHOLD_PERCENT;

        /*
         * threshold for search back pressure service stats
         * currently, only consider the percentage of JVM Usage cancellation count compared to the total cancellation count
         */
        this.heapShardCancellationIncreaseMaxThreshold =
                SearchBackPressureRcaConfig.DEFAULT_SHARD_MAX_HEAP_CANCELLATION_THRESHOLD_PERCENT;
        this.heapTaskCancellationIncreaseMaxThreshold =
                SearchBackPressureRcaConfig.DEFAULT_TASK_MAX_HEAP_CANCELLATION_THRESHOLD_PERCENT;

        this.heapShardCancellationDecreaseMinThreashold =
                SearchBackPressureRcaConfig.DEFAULT_SHARD_MIN_HEAP_CANCELLATION_THRESHOLD_PERCENT;
        this.heapTaskCancellationDecreaseMinThreashold =
                SearchBackPressureRcaConfig.DEFAULT_TASK_MIN_HEAP_CANCELLATION_THRESHOLD_PERCENT;

        // sliding window for heap usage
        this.minHeapUsageSlidingWindow =
                new MinMaxSlidingWindow(
                        SLIDING_WINDOW_SIZE_IN_MINS, TimeUnit.MINUTES, minSlidingWindowNextElement);
        this.maxHeapUsageSlidingWindow =
                new MinMaxSlidingWindow(
                        SLIDING_WINDOW_SIZE_IN_MINS, TimeUnit.MINUTES, maxSlidingWindowNextElement);

        // sliding window for JVM
        this.shardJVMCancellationSlidingWindow =
                new SlidingWindow<>(SLIDING_WINDOW_SIZE_IN_MINS, TimeUnit.MINUTES);
        this.taskJVMCancellationSlidingWindow =
                new SlidingWindow<>(SLIDING_WINDOW_SIZE_IN_MINS, TimeUnit.MINUTES);

        LOG.debug("SearchBackPressureRCA initialized");
    }

    /*
     * generateFlowUnitListFromWire() compute the flow units from other hosts in the cluster
     * for a given Metric and try to send the subscription requests
     * to stale or new hosts in cluster if need be
     */
    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        final List<FlowUnitMessage> flowUnitMessages =
                args.getWireHopper().readFromWire(args.getNode());
        final List<ResourceFlowUnit<HotNodeSummary>> flowUnitList = new ArrayList<>();
        LOG.info(
                "rca: Executing fromWire: {}, remoteFlowUnits: {}",
                this.getClass().getSimpleName(),
                flowUnitMessages);
        for (FlowUnitMessage flowUnitMessage : flowUnitMessages) {
            flowUnitList.add(ResourceFlowUnit.buildFlowUnitFromWrapper(flowUnitMessage));
        }
        setFlowUnits(flowUnitList);
    }

    private long getUpdatedHeapUsedIncreaseThreshold() {
        String val =
                LocalhostConnectionUtil.ClusterSettings.getClusterSettingValue(
                        SEARCH_BACKPRESSURE_HEAP_DURESS_KEY,
                        SEARCH_BACKPRESSURE_HEAP_DURESS_VAL_REGEX);
        // If there was an error fetching the threshold ignore for this run
        if (val.equals(SETTING_NOT_FOUND)) {
            LOG.warn("There was an error fetching the node duress heap settings value...");
            return heapUsedIncreaseThreshold;
        }
        LOG.debug("successfully fetched the node duress heap threshold {}", val);
        // this value will be sub-decimal e,g; 0.7
        double floatVal = Double.parseDouble(val);
        return (long) (floatVal * 100);
    }

    /*
     * operate() evaluates the current stats against threshold
     * Unhealthy Flow Units is a marker that this resource at current instance is not healthy
     * Autotune decision would be made by downstream classes
     */
    @Override
    public ResourceFlowUnit<HotNodeSummary> operate() {
        currentIterationNumber += 1;
        ResourceContext context = null;
        long currentTimeMillis = System.currentTimeMillis();

        // read key metrics into searchBackPressureRCAMetric for easier management
        SearchBackPressureRCAMetric searchBackPressureRCAMetric = getSearchBackPressureRCAMetric();

        // print out oldGenUsed and maxOldGen
        LOG.debug(
                "SearchBackPressureRCA: oldGenUsed: {} maxOldGen: {}, heapUsedPercentage: {}, searchbpShardCancellationCount: {}, searchbpTaskCancellationCount: {}, searchbpJVMShardCancellationCount: {}, searchbpJVMTaskCancellationCount: {}"
                        + ", searchShardTaskCompletionCount: {}, searchTaskCompletionCount: {}",
                searchBackPressureRCAMetric.getUsedHeap(),
                searchBackPressureRCAMetric.getMaxHeap(),
                searchBackPressureRCAMetric.getHeapUsagePercent(),
                searchBackPressureRCAMetric.getSearchbpShardCancellationCount(),
                searchBackPressureRCAMetric.getSearchbpTaskCancellationCount(),
                searchBackPressureRCAMetric.getSearchbpJVMShardCancellationCount(),
                searchBackPressureRCAMetric.getSearchbpJVMTaskCancellationCount(),
                searchBackPressureRCAMetric.getSearchShardTaskCompletionCount(),
                searchBackPressureRCAMetric.getSearchTaskCompletionCount());

        updateAllSlidingWindows(searchBackPressureRCAMetric, currentTimeMillis);

        LOG.debug("SearchBackPressureRCA currentIterationNumber is {}", currentIterationNumber);
        // if currentIterationNumber matches the rca period, emit the flow unit
        if (currentIterationNumber == this.rcaPeriod) {
            LOG.debug(
                    "SearchBackPressureRCA currentIterationNumber in rcaPeriod is {}",
                    currentIterationNumber);
            currentTimeMillis = System.currentTimeMillis();

            // reset currentIterationNumber
            currentIterationNumber = 0;

            heapUsedIncreaseThreshold = getUpdatedHeapUsedIncreaseThreshold();
            // We always want to maintain the gap in increase and decrease thresholds so that OOMs
            // doesn't happen
            // due to excessive traffic
            heapUsedDecreaseThreshold =
                    Math.min(
                            MAX_ALLOWED_HEAP,
                            (long)
                                    (heapUsedIncreaseThreshold
                                            + heapUsedDecreaseThreshold
                                                    * MAX_GAP_BW_BASELINE_HEAP_AND_MAX_ALLOWED));

            double maxHeapUsagePercentage = maxHeapUsageSlidingWindow.readLastElementInWindow();
            double minHeapUsagePercentage = minHeapUsageSlidingWindow.readLastElementInWindow();
            double avgShardJVMCancellationPercentage = shardJVMCancellationSlidingWindow.readAvg();
            double avgTaskJVMCancellationPercentage = taskJVMCancellationSlidingWindow.readAvg();

            LOG.debug(
                    "SearchBackPressureRCA: maxHeapUsagePercentage: {}, minHeapUsagePercentage: {}, SearchBackPressureRCA: avgShardJVMCancellationPercentage: {}, SearchBackPressureRCA: avgTaskJVMCancellationPercentage: {}",
                    maxHeapUsagePercentage,
                    minHeapUsagePercentage,
                    avgShardJVMCancellationPercentage,
                    avgTaskJVMCancellationPercentage);
            InstanceDetails instanceDetails = getInstanceDetails();
            HotNodeSummary nodeSummary =
                    new HotNodeSummary(
                            instanceDetails.getInstanceId(), instanceDetails.getInstanceIp());

            /*
             *  2 cases we send Unhealthy ResourceContext when we need to autotune the threshold
             *  (increase) node max heap usage in last 60 secs is less than 70% and cancellationCountPercentage due to heap is more than 5% of all task completions
             *  (decrease) node min heap usage in last 60 secs is more than 80% and cancellationCountPercetange due to heap is less than 3% of all task completions
             */
            boolean maxHeapBelowIncreaseThreshold =
                    maxHeapUsagePercentage < heapUsedIncreaseThreshold;
            boolean minHeapAboveDecreaseThreshold =
                    minHeapUsagePercentage > heapUsedDecreaseThreshold;
            boolean shardHeapCancellationPercentageAboveThreshold =
                    avgShardJVMCancellationPercentage > heapShardCancellationIncreaseMaxThreshold;
            boolean shardHeapCancellationPercentageBelowThreshold =
                    avgShardJVMCancellationPercentage < heapShardCancellationDecreaseMinThreashold;
            boolean taskHeapCancellationPercentageAboveThreshold =
                    avgTaskJVMCancellationPercentage > heapTaskCancellationIncreaseMaxThreshold;
            boolean taskHeapCancellationPercentageBelowThreshold =
                    avgTaskJVMCancellationPercentage < heapTaskCancellationDecreaseMinThreashold;

            // shard level thresholds
            boolean increaseJVMThresholdMetByShard =
                    maxHeapBelowIncreaseThreshold && shardHeapCancellationPercentageAboveThreshold;
            boolean decreaseJVMThresholdMetByShard =
                    minHeapAboveDecreaseThreshold && shardHeapCancellationPercentageBelowThreshold;

            // task level thresholds
            boolean increaseJVMThresholdMetByTask =
                    maxHeapBelowIncreaseThreshold && taskHeapCancellationPercentageAboveThreshold;
            boolean decreaseJVMThresholdMetByTask =
                    minHeapAboveDecreaseThreshold && taskHeapCancellationPercentageBelowThreshold;

            // Generate a flow unit with an Unhealthy ResourceContext
            if (increaseJVMThresholdMetByShard || decreaseJVMThresholdMetByShard) {
                LOG.debug(
                        "Increase/Decrease Condition Meet for Shard, maxHeapUsagePercentage: {} is less than threshold: {}, avgShardJVMCancellationPercentage: {} is bigger than heapShardCancellationIncreaseMaxThreshold: {}",
                        maxHeapUsagePercentage,
                        heapUsedIncreaseThreshold,
                        avgShardJVMCancellationPercentage,
                        heapShardCancellationIncreaseMaxThreshold);
                context = new ResourceContext(Resources.State.UNHEALTHY);
                HotResourceSummary resourceSummary;
                // metadata fields indicate the reason for Unhealthy Resource Unit
                if (increaseJVMThresholdMetByShard) {
                    resourceSummary =
                            new HotResourceSummary(
                                    SEARCHBACKPRESSURE_SHARD,
                                    0,
                                    0,
                                    0,
                                    SearchBackPressureRcaConfig.INCREASE_THRESHOLD_BY_JVM_STR);
                } else {
                    resourceSummary =
                            new HotResourceSummary(
                                    SEARCHBACKPRESSURE_SHARD,
                                    0,
                                    0,
                                    0,
                                    SearchBackPressureRcaConfig.DECREASE_THRESHOLD_BY_JVM_STR);
                }

                nodeSummary.appendNestedSummary(resourceSummary);
                return new ResourceFlowUnit<>(
                        currentTimeMillis,
                        context,
                        nodeSummary,
                        !instanceDetails.getIsClusterManager());
            } else if (increaseJVMThresholdMetByTask || decreaseJVMThresholdMetByTask) {
                LOG.debug(
                        "Increase/Decrease Condition Meet for Task, maxHeapUsagePercentage: {} is less than threshold: {}, avgShardJVMCancellationPercentage: {} is bigger than heapShardCancellationIncreaseMaxThreshold: {}",
                        maxHeapUsagePercentage,
                        heapUsedIncreaseThreshold,
                        avgTaskJVMCancellationPercentage,
                        heapTaskCancellationIncreaseMaxThreshold);

                context = new ResourceContext(Resources.State.UNHEALTHY);
                HotResourceSummary resourceSummary;
                if (increaseJVMThresholdMetByTask) {
                    resourceSummary =
                            new HotResourceSummary(
                                    SEARCHBACKPRESSURE_TASK,
                                    0,
                                    0,
                                    0,
                                    SearchBackPressureRcaConfig.INCREASE_THRESHOLD_BY_JVM_STR);
                } else {
                    resourceSummary =
                            new HotResourceSummary(
                                    SEARCHBACKPRESSURE_TASK,
                                    0,
                                    0,
                                    0,
                                    SearchBackPressureRcaConfig.DECREASE_THRESHOLD_BY_JVM_STR);
                }

                nodeSummary.appendNestedSummary(resourceSummary);
                return new ResourceFlowUnit<>(
                        currentTimeMillis,
                        context,
                        nodeSummary,
                        !instanceDetails.getIsClusterManager());
            } else {
                // if autotune is not triggered, return healthy state
                context = new ResourceContext(Resources.State.HEALTHY);
                return new ResourceFlowUnit<>(
                        currentTimeMillis,
                        context,
                        nodeSummary,
                        !instanceDetails.getIsClusterManager());
            }

        } else {
            // Return Empty ResourceFlowUnit if none of the thresholds is met
            LOG.debug("Empty FlowUnit returned for SearchbackPressureRCA");
            currentTimeMillis = System.currentTimeMillis();
            return new ResourceFlowUnit<>(currentTimeMillis);
        }
    }

    /**
     * Get the Heap Related Stats (Heap Used and Heap Size in gigabytes)
     *
     * @param isHeapUsed is true meaning get the value of used heap in gigabytes otherwise, meaning
     *     get the value of max heap in gigabytes
     */
    public double getHeapStats(boolean isHeapUsed) {
        double heapStats = DEFAULT_HEAP_VAL;
        List<MetricFlowUnit> heapStatsMetrics;
        if (isHeapUsed == true) {
            if (heapUsed == null) {
                throw new IllegalStateException(
                        "RCA: "
                                + this.name()
                                + "was not configured in the graph to "
                                + "take heap_Used as a metric. Please check the analysis graph!");
            }

            heapStatsMetrics = heapUsed.getFlowUnits();
        } else {
            if (heapMax == null) {
                throw new IllegalStateException(
                        "RCA: "
                                + this.name()
                                + "was not configured in the graph to "
                                + "take heap_Max as a metric. Please check the analysis graph!");
            }

            heapStatsMetrics = heapMax.getFlowUnits();
        }

        for (MetricFlowUnit heapStatsMetric : heapStatsMetrics) {
            if (heapStatsMetric.isEmpty()) {
                continue;
            }

            double ret =
                    SQLParsingUtil.readDataFromSqlResult(
                            heapStatsMetric.getData(),
                            AllMetrics.HeapDimension.MEM_TYPE.getField(),
                            AllMetrics.GCType.HEAP.toString(),
                            MetricsDB.MAX);
            if (Double.isNaN(ret)) {
                LOG.error(
                        "Failed to parse metric in FlowUnit from {}",
                        heapUsed.getClass().getName());
            } else {
                heapStats = ret / CONVERT_BYTES_TO_MEGABYTES;
            }
        }

        return heapStats;
    }

    private SearchBackPressureRCAMetric getSearchBackPressureRCAMetric() {
        // Get Heap Usage related metrics
        double prevHeapUsage = getHeapStats(true);
        double maxHeapSize = getHeapStats(false);

        // Log prevHeapUsage and maxHeapSize
        LOG.debug("prevHeapUsage: {}, maxHeapSize: {}", prevHeapUsage, maxHeapSize);

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
                                .SEARCHBP_SHARD_TASK_STATS_CANCELLATION_COUNT
                                .toString());
        double searchbpTaskCancellationCount =
                getMetric(
                        this.searchbp_Stats,
                        searchbp_stats_type_field,
                        AllMetrics.SearchBackPressureStatsValue
                                .SEARCHBP_SEARCH_TASK_STATS_CANCELLATION_COUNT
                                .toString());
        double searchShardTaskCompletionCount =
                getMetric(
                        this.searchbp_Stats,
                        searchbp_stats_type_field,
                        AllMetrics.SearchBackPressureStatsValue
                                .SEARCHBP_SHARD_TASK_STATS_COMPLETION_COUNT
                                .toString());
        double searchTaskCompletionCount =
                getMetric(
                        this.searchbp_Stats,
                        searchbp_stats_type_field,
                        AllMetrics.SearchBackPressureStatsValue
                                .SEARCHBP_SEARCH_TASK_STATS_COMPLETION_COUNT
                                .toString());
        double searchbpJVMShardCancellationCount =
                getMetric(
                        this.searchbp_Stats,
                        searchbp_stats_type_field,
                        AllMetrics.SearchBackPressureStatsValue
                                .SEARCHBP_SHARD_TASK_STATS_RESOURCE_HEAP_USAGE_CANCELLATION_COUNT
                                .toString());
        double searchbpJVMTaskCancellationCount =
                getMetric(
                        this.searchbp_Stats,
                        searchbp_stats_type_field,
                        AllMetrics.SearchBackPressureStatsValue
                                .SEARCHBP_SEARCH_TASK_STATS_RESOURCE_HEAP_USAGE_CANCELLATION_COUNT
                                .toString());

        return new SearchBackPressureRCAMetric(
                prevHeapUsage,
                maxHeapSize,
                searchbpShardCancellationCount,
                searchbpTaskCancellationCount,
                searchShardTaskCompletionCount,
                searchTaskCompletionCount,
                searchbpJVMShardCancellationCount,
                searchbpJVMTaskCancellationCount);
    }

    private <M extends Metric> double getMetric(M metric, Field<String> field, String fieldName) {
        if (metric == null) {
            throw new IllegalStateException(
                    "RCA: "
                            + this.name()
                            + "was not configured in the graph to "
                            + "take "
                            + metric.name()
                            + " as a metric. Please check the analysis graph!");
        }

        double response = 0.0;
        for (MetricFlowUnit flowUnit : metric.getFlowUnits()) {
            if (!flowUnit.isEmpty()) {
                double metricResponse =
                        readDataFromSqlResult(flowUnit.getData(), field, fieldName, MetricsDB.MAX);
                LOG.debug("Searchbp metricResponse is: {}", metricResponse);
                if (!Double.isNaN(metricResponse) && metricResponse >= 0.0) {
                    response = metricResponse;
                }
            }
        }
        LOG.debug("Searchbp response is: {}", response);
        return response;
    }

    /**
     * read threshold values from rca.conf
     *
     * @param conf RcaConf object
     */
    @Override
    public void readRcaConf(RcaConf conf) {
        final SearchBackPressureRcaConfig config = conf.getSearchBackPressureRcaConfig();

        // threshold read from config file
        this.heapUsedIncreaseThreshold = config.getMaxHeapIncreasePercentageThreshold();
        LOG.debug(
                "SearchBackPressureRCA heapUsedIncreaseThreshold is set to {}",
                this.heapUsedIncreaseThreshold);
        this.heapShardCancellationIncreaseMaxThreshold =
                config.getMaxShardHeapCancellationPercentageThreshold();
        this.heapTaskCancellationIncreaseMaxThreshold =
                config.getMaxTaskHeapCancellationPercentageThreshold();
        this.heapUsedDecreaseThreshold = config.getMinHeapDecreasePercentageThreshold();
        this.heapShardCancellationDecreaseMinThreashold =
                config.getMinShardHeapCancellationPercentageThreshold();
        this.heapTaskCancellationDecreaseMinThreashold =
                config.getMinTaskHeapCancellationPercentageThreshold();
    }

    /*
     * Update Stats for all Sliding Windows
     */
    private void updateAllSlidingWindows(
            SearchBackPressureRCAMetric searchBackPressureRCAMetric, long currentTimeMillis) {
        double prevHeapUsagePercentage = searchBackPressureRCAMetric.getHeapUsagePercent();
        if (!Double.isNaN(prevHeapUsagePercentage)) {
            minHeapUsageSlidingWindow.next(
                    new SlidingWindowData(currentTimeMillis, prevHeapUsagePercentage));
            maxHeapUsageSlidingWindow.next(
                    new SlidingWindowData(currentTimeMillis, prevHeapUsagePercentage));
        }

        double shardJVMCancellationPercentage =
                searchBackPressureRCAMetric.getShardJVMCancellationPercent();
        if (!Double.isNaN(shardJVMCancellationPercentage)) {
            shardJVMCancellationSlidingWindow.next(
                    new SlidingWindowData(currentTimeMillis, shardJVMCancellationPercentage));
        }

        double taskJVMCancellationPercentage =
                searchBackPressureRCAMetric.getTaskJVMCancellationPercent();
        if (!Double.isNaN(taskJVMCancellationPercentage)) {
            taskJVMCancellationSlidingWindow.next(
                    new SlidingWindowData(currentTimeMillis, taskJVMCancellationPercentage));
        }
    }
}
