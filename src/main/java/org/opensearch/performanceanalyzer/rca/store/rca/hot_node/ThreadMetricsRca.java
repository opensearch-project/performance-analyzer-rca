/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.hot_node;

import com.google.common.annotations.VisibleForTesting;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
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
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Thread_Blocked_Time;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Thread_Waited_Time;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ReaderMetrics;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;

import static org.opensearch.performanceanalyzer.rca.framework.metrics.ReaderMetrics.BLOCKED_TRANSPORT_THREAD_COUNT;
import static org.opensearch.performanceanalyzer.rca.framework.metrics.ReaderMetrics.WAITED_TRANSPORT_THREAD_COUNT;
import static org.opensearch.performanceanalyzer.rca.framework.metrics.ReaderMetrics.MAX_TRANSPORT_THREAD_BLOCKED_TIME;
import static org.opensearch.performanceanalyzer.rca.framework.metrics.ReaderMetrics.MAX_TRANSPORT_THREAD_WAITED_TIME;

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

    @Override
    public void readRcaConf(RcaConf conf) {}

    @Override
    public ResourceFlowUnit<HotNodeSummary> operate() {
        try {
            long currentTimeMillis = this.clock.millis();
            LOG.debug("ThreadMetricsRca run at {}", currentTimeMillis);
            collateThreadMetricData(currentTimeMillis);
            publishStats();

        } catch (Exception e) {
            LOG.error("ThreadMetricsRca.operate() Failed", e);
        }
        return new ResourceFlowUnit<>(clock.millis());
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
                    LOG.error("ThreadMetricsRca.operate() Failed to parse data for record "
                            + record.formatJSON(), e);
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
                                    .filter(tm -> analysis.getTypeFilter().test(tm.operation))
                                    .collect(Collectors.toList()));
                });
    }

    public static class ThreadMetricsSlidingWindow {

        private final Map<String, Deque<ThreadMetric>> metricsByThreadName;
        private final Map<String, Double> metricSumMap;
        private static final long SLIDING_WINDOW_SIZE_IN_SECONDS = 60;

        public ThreadMetricsSlidingWindow() {
            metricsByThreadName = new HashMap<>();
            metricSumMap = new HashMap<>();
        }

        /** insert data into the sliding window */
        public void next(long timestamp, List<ThreadMetric> threadMetricList) {
            Set<String> newThreadNames = new HashSet<>();
            for (ThreadMetric tm : threadMetricList) {
                Deque<ThreadMetric> windowDeque;
                if (metricsByThreadName.containsKey(tm.getName())) {
                    windowDeque = metricsByThreadName.get(tm.getName());
                    pruneExpiredEntries(tm.getTimeStamp(), windowDeque);
                } else {
                    windowDeque = new LinkedList<>();
                    metricsByThreadName.put(tm.name, windowDeque);
                }
                windowDeque.addFirst(tm);
                addValue(tm);
                newThreadNames.add(tm.name);
            }

            for (Map.Entry<String, Deque<ThreadMetric>> entry : metricsByThreadName.entrySet()) {
                if (newThreadNames.contains(entry.getKey())) {
                    continue;
                }
                pruneExpiredEntries(timestamp, entry.getValue());
            }
            metricsByThreadName.entrySet().removeIf(e -> e.getValue().size() == 0);
        }

        private void pruneExpiredEntries(long endTimeStamp, Deque<ThreadMetric> windowDeque) {
            while (!windowDeque.isEmpty()
                    && TimeUnit.MILLISECONDS.toSeconds(
                                    endTimeStamp - windowDeque.peekLast().getTimeStamp())
                            > SLIDING_WINDOW_SIZE_IN_SECONDS) {
                // remove from window
                ThreadMetric prunedData = windowDeque.pollLast();
                // update blocked time sum for thread in new window
                if (prunedData != null) {
                    removeValue(prunedData);
                }
            }
        }

        private void removeValue(ThreadMetric prunedData) {
            updateValue(prunedData, false);
        }

        private void addValue(ThreadMetric prunedData) {
            updateValue(prunedData, true);
        }

        private void updateValue(ThreadMetric tm, boolean add) {
            String threadName = tm.name;
            if (metricSumMap.containsKey(threadName)) {
                double sign = add ? 1d : -1d;
                double newVal = metricSumMap.get(threadName) + sign * tm.getValue();
                if (newVal == 0) {
                    metricSumMap.remove(threadName);
                } else {
                    metricSumMap.put(threadName, newVal);
                }
            } else if (add) {
                metricSumMap.put(threadName, tm.getValue());
            }
        }

        public int getCountExceedingThreshold(double threshold) {
            return (int) metricSumMap.values().stream().filter(val -> val > threshold).count();
        }

        public double getMaxSum() {
            return metricSumMap.size() > 0 ? Collections.max(metricSumMap.values()) : 0d;
        }
    }

    public static class ThreadMetric {
        private final String name;
        private final double value;
        private final long timeStamp;

        private final String operation;

        public ThreadMetric(String threadName, double val, long timeStamp, String operation) {
            this.name = threadName;
            this.value = val;
            this.timeStamp = timeStamp;
            this.operation = operation;
        }

        public String getOperation() {
            return operation;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public String getName() {
            return name;
        }

        public double getValue() {
            return value;
        }
    }

    public static class ThreadAnalysis {
        public ThreadMetricsSlidingWindow getBlockedTimeWindow() {
            return blockedTimeWindow;
        }

        public ThreadMetricsSlidingWindow getWaitedTimeWindow() {
            return waitedTimeWindow;
        }

        public Predicate<String> getTypeFilter() {
            return typeFilter;
        }

        public ReaderMetrics getBlockedThreadCountMetric() {
            return blockedThreadCountMetric;
        }

        public ReaderMetrics getWaitedThreadCountMetric() {
            return waitedThreadCountMetric;
        }

        public ReaderMetrics getMaxBlockedTimeMetric() {
            return maxBlockedTimeMetric;
        }

        public ReaderMetrics getMaxWaitedTimeMetric() {
            return maxWaitedTimeMetric;
        }

        private final ThreadMetricsSlidingWindow blockedTimeWindow;
        private final ThreadMetricsSlidingWindow waitedTimeWindow;
        private final Predicate<String> typeFilter;
        private final ReaderMetrics blockedThreadCountMetric,
                waitedThreadCountMetric,
                maxBlockedTimeMetric,
                maxWaitedTimeMetric;

        public ThreadAnalysis(
                Predicate<String> typeFilter,
                ReaderMetrics blockedThreadCountMetric,
                ReaderMetrics waitedThreadCount,
                ReaderMetrics maxBlockedTime,
                ReaderMetrics maxWaitedTimeMetric) {
            this.typeFilter = typeFilter;
            this.blockedThreadCountMetric = blockedThreadCountMetric;
            this.waitedThreadCountMetric = waitedThreadCount;
            this.maxBlockedTimeMetric = maxBlockedTime;
            this.maxWaitedTimeMetric = maxWaitedTimeMetric;
            blockedTimeWindow = new ThreadMetricsSlidingWindow();
            waitedTimeWindow = new ThreadMetricsSlidingWindow();
        }
    }
}
