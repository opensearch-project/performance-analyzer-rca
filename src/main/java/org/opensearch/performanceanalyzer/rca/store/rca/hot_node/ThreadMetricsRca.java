/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.hot_node;

import static org.opensearch.performanceanalyzer.rca.framework.metrics.ReaderMetrics.*;

import com.google.common.annotations.VisibleForTesting;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Record;
import org.jooq.Result;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Thread_Blocked_Time;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Thread_Waited_Time;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;

/**
 * Analyzes thread_blocked_time and thread_waited_time metrics for all threads and in specific
 * transport threads and publishes the following data: 1. Number of threads which are blocked for
 * more than 5 seconds in past minute. 2. Number of threads which are in waiting/timed-waiting state
 * for more than 5 seconds in past minute. 3. Maximum value of thread_blocked_time for a single
 * thread in the past minute. 4. Maximum value of thread_waited_time for a single thread in the past
 * minute.
 */
public class ThreadMetricsRca extends Rca<ResourceFlowUnit<HotNodeSummary>> {

    private static final Logger LOG = LogManager.getLogger(ThreadMetricsRca.class);
    public static final double HIGH_BLOCKED_TIME_THRESHOLD_IN_SECONDS = 5d;
    private final int rcaPeriod;
    private final Thread_Blocked_Time threadBlockedTime;
    private final Thread_Waited_Time threadWaitedTime;
    private final Clock clock;
    @VisibleForTesting final List<ThreadAnalysis> threadAnalyses;

    public ThreadMetricsRca(
            final Thread_Blocked_Time threadBlockedTime,
            final Thread_Waited_Time threadWaitedTime,
            final int rcaPeriodInSeconds) {
        super(rcaPeriodInSeconds);
        this.rcaPeriod = rcaPeriodInSeconds;
        this.threadBlockedTime = threadBlockedTime;
        this.threadWaitedTime = threadWaitedTime;
        threadAnalyses = new ArrayList<>();
        initThreadAnalyses();
        this.clock = Clock.systemUTC();
    }

    private void initThreadAnalyses() {
        // transport threads
        threadAnalyses.add(
                new ThreadAnalysis(
                        s -> s.contains("transport"),
                        BLOCKED_TRANSPORT_THREAD_COUNT,
                        WAITED_TRANSPORT_THREAD_COUNT,
                        MAX_TRANSPORT_THREAD_BLOCKED_TIME,
                        MAX_TRANSPORT_THREAD_WAITED_TIME));
    }

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

    /**
     * This RCA will be consumed by hot node rca as threads which are blocked beyond a threshold
     * indicate a hot node. Until we calibrate a reasonable value for threshold of blocked time in
     * threads we will set the RCA as healthy to ensure we don't hinder the decisioning of hot node
     * rca.
     */
    @Override
    public ResourceFlowUnit<HotNodeSummary> operate() {
        try {
            InstanceDetails instanceDetails = getInstanceDetails();
            long currentTimeMillis = this.clock.millis();
            LOG.debug("ThreadMetricsRca run at {}", currentTimeMillis);
            collateThreadMetricData(currentTimeMillis);
            publishStats();
            return new ResourceFlowUnit<>(
                    clock.millis(),
                    new ResourceContext(Resources.State.HEALTHY),
                    new HotNodeSummary(
                            instanceDetails.getInstanceId(), instanceDetails.getInstanceIp()),
                    false);
        } catch (Exception e) {
            LOG.error("ThreadMetricsRca.operate() Failed", e);
            return new ResourceFlowUnit<>(clock.millis());
        }
    }

    private void publishStats() {
        threadAnalyses.forEach(
                analysis -> {
                    PerformanceAnalyzerApp.READER_METRICS_AGGREGATOR.updateStat(
                            analysis.getBlockedThreadCountMetric(),
                            "",
                            analysis.getBlockedTimeWindow()
                                    .getCountExceedingThreshold(
                                            HIGH_BLOCKED_TIME_THRESHOLD_IN_SECONDS));

                    PerformanceAnalyzerApp.READER_METRICS_AGGREGATOR.updateStat(
                            analysis.getMaxBlockedTimeMetric(),
                            "",
                            analysis.getBlockedTimeWindow().getMaxSum());

                    PerformanceAnalyzerApp.READER_METRICS_AGGREGATOR.updateStat(
                            analysis.getWaitedThreadCountMetric(),
                            "",
                            analysis.getWaitedTimeWindow()
                                    .getCountExceedingThreshold(
                                            HIGH_BLOCKED_TIME_THRESHOLD_IN_SECONDS));

                    PerformanceAnalyzerApp.READER_METRICS_AGGREGATOR.updateStat(
                            analysis.getMaxWaitedTimeMetric(),
                            "",
                            analysis.getWaitedTimeWindow().getMaxSum());
                });
    }

    private void collateThreadMetricData(long currentTimeMillis) {
        collateThreadMetricData(
                currentTimeMillis, threadBlockedTime, ThreadAnalysis::getBlockedTimeWindow);
        collateThreadMetricData(
                currentTimeMillis, threadWaitedTime, ThreadAnalysis::getWaitedTimeWindow);
    }

    private void collateThreadMetricData(
            long currentTimeMillis,
            Metric metric,
            Function<ThreadAnalysis, ThreadMetricsSlidingWindow> slidingWindowFunction) {
        ArrayList<ThreadMetric> threadList = new ArrayList<>();
        for (MetricFlowUnit flowUnit : metric.getFlowUnits()) {
            if (flowUnit.isEmpty()) {
                continue;
            }
            final Result<Record> result = flowUnit.getData();
            if (result == null) {
                continue;
            }
            for (Record record : result) {
                Object nameObj = record.get(AllMetrics.CommonDimension.THREAD_NAME.toString());
                Object operationObj = record.get(AllMetrics.CommonDimension.OPERATION.toString());
                Object valObj = record.get(MetricsDB.AVG);
                if (nameObj == null || operationObj == null || valObj == null) {
                    continue;
                }
                try {
                    String operation = operationObj.toString();
                    String threadName = nameObj.toString();
                    double val = Double.parseDouble(valObj.toString());
                    if (val > 0) {
                        ThreadMetric tm =
                                new ThreadMetric(threadName, val, currentTimeMillis, operation);
                        threadList.add(tm);
                    }
                } catch (Exception e) {
                    LOG.error(
                            "ThreadMetricsRca.operate() Failed to parse data for record "
                                    + record.formatJSON(),
                            e);
                    break;
                }
            }
        }
        threadAnalyses.forEach(
                analysis -> {
                    ThreadMetricsSlidingWindow slidingWindow =
                            slidingWindowFunction.apply(analysis);
                    slidingWindow.next(
                            currentTimeMillis,
                            threadList.stream()
                                    .filter(tm -> analysis.getTypeFilter().test(tm.getOperation()))
                                    .collect(Collectors.toList()));
                });
    }
}
