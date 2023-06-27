/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.searchbackpressure;

import static org.opensearch.performanceanalyzer.rca.framework.api.persist.SQLParsingUtil.readDataFromSqlResult;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.configs.SearchBackPressureRcaConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;
import org.opensearch.performanceanalyzer.rca.store.rca.OldGenRca;

public class SearchBackPressureRCA extends OldGenRca<ResourceFlowUnit<HotNodeSummary>> {
    // LOGGER for SearchBackPressureRCA
    private static final Logger LOG = LogManager.getLogger(SearchBackPressureRCA.class);
    private static final double BYTES_TO_GIGABYTES = Math.pow(1024, 3);
    private static final long EVAL_INTERVAL_IN_S = 5;

    // Key Metrics to be used to determine health status
    // Task Level cancellationCount
    // Shard Level cancellationCount
    // Task level max heap usage
    // Shard level max heap usage
    // total node heap usage
    private final Metric heapUsed;
    private final Metric searchbp_Stats;

    private long SearchBPCancellationJVMThreshold;

    // cases to incrase threshold
    private long heapUsedIncreaseMaxThreshold;
    private long heapCancellationIncreaseMaxThreshold;

    // case to decrease threshold
    private long heapUsedDecreaseMinThreshold;
    private long heapCancellationDecreaseMaxThreashold;

    // Period: 60s

    // track how many samples has been checked (only reach 60s (12 * 5s) to execute
    // operate())
    private long counter;

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
            final M heapMax, final M heapUsed, M gcType, M searchbp_Stats) {
        super(EVAL_INTERVAL_IN_S, heapUsed, heapMax, null, gcType);
        this.heapUsed = heapUsed;
        this.searchbp_Stats = searchbp_Stats;
        this.heapUsedIncreaseMaxThreshold =
                SearchBackPressureRcaConfig.DEFAULT_MAX_HEAP_DOWNFLOW_THRESHOLD;
        this.heapCancellationIncreaseMaxThreshold =
                SearchBackPressureRcaConfig.DEFAULT_MAX_HEAP_CANCELLATION_THRESHOLD;
        this.heapUsedDecreaseMinThreshold =
                SearchBackPressureRcaConfig.DEFAULT_MIN_HEAP_OVERFLOW_THRESHOLD;
        this.heapCancellationDecreaseMaxThreashold =
                SearchBackPressureRcaConfig.DEFAULT_MIN_HEAP_CANCELLATION_THRESHOLD;

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
        // Use OldGenRca.java to get heap usage and max heap size
        double prevHeapUsage = getOldGenUsedOrDefault(0d);
        double maxHeapSize = getMaxOldGenSizeOrDefault(Double.MAX_VALUE);

        double heapUsedPercentage = prevHeapUsage / maxHeapSize;

        // function to read cancellation count from sql
        getSearchBackPressureShardCancellationCount();

        // print out oldGenUsed and maxOldGen
        LOG.info(
                "SearchBackPressureRCA: oldGenUsed: {} maxOldGen: {}, heapUsedPercentage: {}",
                prevHeapUsage,
                maxHeapSize,
                heapUsedPercentage);
        LOG.info("SearchBackPressureRCA operate() finished");
        return null;
    }

    private long getSearchBackPressureShardCancellationCount() {
        LOG.info("getSearchBackPressureShardCancellationCount() STARTED");

        // Use Searchbp_Stats metrics to get the metrics value
        // shard level cancellation count
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
        LOG.info(
                "SearchBackPressureRCA: searchbpShardCancellationCount: {}",
                searchbpShardCancellationCount);
        // print out searchbpTaskCancellationCount, searchbpJVMShardCancellationCount,
        // searchbpJVMTaskCancellationCount
        LOG.info(
                "SearchBackPressureRCA: searchbpTaskCancellationCount: {}",
                searchbpTaskCancellationCount);
        LOG.info(
                "SearchBackPressureRCA: searchbpJVMShardCancellationCount: {}",
                searchbpJVMShardCancellationCount);
        LOG.info(
                "SearchBackPressureRCA: searchbpJVMTaskCancellationCount: {}",
                searchbpJVMTaskCancellationCount);

        LOG.info("getSearchBackPressureShardCancellationCount() finished");
        return 0;
    }

    private long getSearchBackPressureTaskCancellationCount() {
        return 0;
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
