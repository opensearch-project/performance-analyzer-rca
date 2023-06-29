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
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.SlidingWindow;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.SlidingWindowData;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
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
    private final MinMaxOldGenSlidingWindow heapUsageSlidingWindow;

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

    public <M extends Metric> SearchBackPressureRCA(
            final int rcaPeriod, final M heapMax, final M heapUsed, M gcType, M searchbp_Stats) {
        super(EVAL_INTERVAL_IN_S, heapUsed, heapMax, null, gcType);
        this.heapUsed = heapUsed;
        this.rcaPeriod = rcaPeriod;
        this.clock = Clock.systemUTC();
        this.searchbp_Stats = searchbp_Stats;
        this.heapUsedIncreaseThreshold =
                SearchBackPressureRcaConfig.DEFAULT_MAX_HEAP_INCREASE_THRESHOLD;
        this.heapShardCancellationIncreaseMaxThreshold =
                SearchBackPressureRcaConfig.DEFAULT_SHARD_MAX_HEAP_CANCELLATION_THRESHOLD;
        this.heapTaskCancellationIncreaseMaxThreshold =
                SearchBackPressureRcaConfig.DEFAULT_TASK_MAX_HEAP_CANCELLATION_THRESHOLD;
        this.heapUsedDecreaseThreshold =
                SearchBackPressureRcaConfig.DEFAULT_MIN_HEAP_DECREASE_THRESHOLD;
        this.heapShardCancellationDecreaseMinThreashold =
                SearchBackPressureRcaConfig.DEFAULT_SHARD_MIN_HEAP_CANCELLATION_THRESHOLD;
        this.heapTaskCancellationDecreaseMinThreashold =
                SearchBackPressureRcaConfig.DEFAULT_TASK_MIN_HEAP_CANCELLATION_THRESHOLD;

        // initialize sliding window
        this.heapUsageSlidingWindow =
                new MinMaxOldGenSlidingWindow(SLIDING_WINDOW_SIZE_IN_MINS, TimeUnit.MINUTES);
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
        ResourceContext context = null;
        long currentTimeMillis = System.currentTimeMillis();
        ;

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
                    new SlidingWindowData(currentTimeMillis, prevheapUsagePercentage));
        }

        // for testing
        // heapUsageSlidingWindow.next(new SlidingWindowData(currentTimeMillis, 65.3));

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

        LOG.info("SearchBackPressureRCA counter is {}", counter);
        // if counter matches the rca period, emit the flow unit
        if (counter == this.rcaPeriod) {
            LOG.info("SearchBackPressureRCA counter in rcaPeriod is {}", counter);
            currentTimeMillis = System.currentTimeMillis();

            // reset counter
            counter = 0;

            double maxHeapUsagePercentage = heapUsageSlidingWindow.readMax();
            double minHeapUsagePercentage = heapUsageSlidingWindow.readMin();
            double avgShardJVMCancellationPercentage = shardJVMCancellationSlidingWindow.readAvg();
            double avgTaskJVMCancellationPercentage = taskJVMCancellationSlidingWindow.readAvg();

            LOG.info(
                    "SearchBackPressureRCA: maxHeapUsagePercentage: {}, minHeapUsagePercentage: {}, SearchBackPressureRCA: avgShardJVMCancellationPercentage: {}, SearchBackPressureRCA: avgTaskJVMCancellationPercentage: {}",
                    maxHeapUsagePercentage,
                    minHeapUsagePercentage,
                    avgShardJVMCancellationPercentage,
                    avgTaskJVMCancellationPercentage);
            InstanceDetails instanceDetails = getInstanceDetails();
            HotNodeSummary nodeSummary =
                    new HotNodeSummary(
                            instanceDetails.getInstanceId(), instanceDetails.getInstanceIp());

            // get the Configured Threshold and compare with Sliding Window Stats
            /*
             *  2 cases we send Unhealthy ResourceContext when we need to autotune the threshold
             *  - (increase) node max heap usage in last 60 secs is less than 70% and cancellationCountPercentage due to heap is more than 50% of all task cancellations
             *  - (decrease) node min heap usage in last 60 secs is more than 80% and cancellationCountPercetange due to heap is less than 30% of all task cancellations
             */
            //     avgShardJVMCancellationPercentage = 80.0; // testing

            // TODO: add Task CancellationCountPercentage as another criteria
            // TODO
            /*
            *      HotResourceSummary resourceSummary =
                   new HotResourceSummary(HEAP_MAX_SIZE, currentThreshold, previousThreshold, 0);
                   nodeSummary.appendNestedSummary(resourceSummary);

                   If you
            */
            boolean increaseThresholdMetByShard =
                    (maxHeapUsagePercentage < heapUsedIncreaseThreshold)
                            && (avgShardJVMCancellationPercentage
                                    > heapShardCancellationIncreaseMaxThreshold);
            boolean decreaseThresholdMetByShard =
                    (minHeapUsagePercentage > heapUsedDecreaseThreshold)
                            && (avgShardJVMCancellationPercentage
                                    < heapShardCancellationDecreaseMinThreashold);

            boolean increaseThresholdMetByTask =
                    (maxHeapUsagePercentage < heapUsedIncreaseThreshold)
                            && (avgTaskJVMCancellationPercentage
                                    > heapTaskCancellationIncreaseMaxThreshold);
            boolean decreaseThresholdMetByTask =
                    (minHeapUsagePercentage > heapUsedDecreaseThreshold)
                            && (avgTaskJVMCancellationPercentage
                                    < heapTaskCancellationDecreaseMinThreashold);

            // HotResourceSummary resourceSummary =
            //             new HotResourceSummary(HEAP_MAX_SIZE, currentThreshold,
            // previousThreshold, 0);
            //             nodeSummary.appendNestedSummary(resourceSummary);

            if (increaseThresholdMetByShard || decreaseThresholdMetByShard) {
                // Generate a flow unit with an Unhealthy ResourceContext
                LOG.info(
                        "Increase/Decrease Condition Meet for Shard, maxHeapUsagePercentage: {} is less than threshold: {}, avgShardJVMCancellationPercentage: {} is bigger than heapShardCancellationIncreaseMaxThreshold: {}",
                        maxHeapUsagePercentage,
                        heapUsedIncreaseThreshold,
                        avgShardJVMCancellationPercentage,
                        heapShardCancellationIncreaseMaxThreshold);

                context = new ResourceContext(Resources.State.UNHEALTHY);
                // add an additional resource with metadata: shard-level
                return new ResourceFlowUnit<>(
                        currentTimeMillis,
                        context,
                        nodeSummary,
                        !instanceDetails.getIsClusterManager());
            } else if (increaseThresholdMetByTask || decreaseThresholdMetByTask) {
                // Generate a flow unit with an Unhealthy ResourceContext
                LOG.info(
                        "Increase/Decrease Condition Meet for Task, maxHeapUsagePercentage: {} is less than threshold: {}, avgShardJVMCancellationPercentage: {} is bigger than heapShardCancellationIncreaseMaxThreshold: {}",
                        maxHeapUsagePercentage,
                        heapUsedIncreaseThreshold,
                        avgTaskJVMCancellationPercentage,
                        heapTaskCancellationIncreaseMaxThreshold);

                context = new ResourceContext(Resources.State.UNHEALTHY);
                // add an additional resource with metadata: task-level
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
            // return healthy state when the counter does not meet rcaPeriod
            LOG.info("Empty Healthy FlowUnit returned for SearchbackPressureRCA");
            currentTimeMillis = System.currentTimeMillis();
            return new ResourceFlowUnit<>(currentTimeMillis);
        }
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
        this.heapUsedIncreaseThreshold = config.getMaxHeapIncreasePercentageThreshold();
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
}
