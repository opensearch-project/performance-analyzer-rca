/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.collectors;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.rca.framework.metrics.WriterMetrics;

public class ScheduledMetricCollectorsExecutor extends Thread {
    private static final Logger LOG = LogManager.getLogger(ScheduledMetricCollectorsExecutor.class);
    private final int collectorThreadCount;
    private static final int DEFAULT_COLLECTOR_THREAD_COUNT = 5;
    private static final int COLLECTOR_THREAD_KEEPALIVE_SECS = 1000;
    private final boolean checkFeatureDisabledFlag;
    private boolean paEnabled = false;
    private boolean threadContentionMonitoringEnabled = false;
    private int minTimeIntervalToSleep = Integer.MAX_VALUE;
    private Map<PerformanceAnalyzerMetricsCollector, Long> metricsCollectors;

    public static final String COLLECTOR_THREAD_POOL_NAME = "pa-collectors-th";

    private ThreadPoolExecutor metricsCollectorsTP;

    public ScheduledMetricCollectorsExecutor(
            int collectorThreadCount, boolean checkFeatureDisabledFlag) {
        metricsCollectors = new HashMap<>();
        metricsCollectorsTP = null;
        this.collectorThreadCount = collectorThreadCount;
        this.checkFeatureDisabledFlag = checkFeatureDisabledFlag;
    }

    public ScheduledMetricCollectorsExecutor() {
        this(DEFAULT_COLLECTOR_THREAD_COUNT, true);
    }

    public synchronized void setEnabled(final boolean enabled) {
        paEnabled = enabled;
    }

    public synchronized boolean getEnabled() {
        return paEnabled;
    }

    public synchronized void setThreadContentionMonitoringEnabled(final boolean enabled) {
        metricsCollectors
                .keySet()
                .forEach(collector -> collector.setThreadContentionMonitoringEnabled(enabled));
        threadContentionMonitoringEnabled = enabled;
    }

    private synchronized boolean getThreadContentionMonitoringEnabled() {
        return threadContentionMonitoringEnabled;
    }

    public void addScheduledMetricCollector(PerformanceAnalyzerMetricsCollector task) {
        task.setThreadContentionMonitoringEnabled(getThreadContentionMonitoringEnabled());
        metricsCollectors.put(task, System.currentTimeMillis() + task.getTimeInterval());
        if (task.getTimeInterval() < minTimeIntervalToSleep) {
            minTimeIntervalToSleep = task.getTimeInterval();
        }
    }

    public void run() {
        Thread.currentThread().setName(this.getClass().getSimpleName());
        if (metricsCollectorsTP == null) {
            ThreadFactory taskThreadFactory =
                    new ThreadFactoryBuilder()
                            .setNameFormat(COLLECTOR_THREAD_POOL_NAME)
                            .setDaemon(true)
                            .build();
            metricsCollectorsTP =
                    new ThreadPoolExecutor(
                            collectorThreadCount,
                            collectorThreadCount,
                            COLLECTOR_THREAD_KEEPALIVE_SECS,
                            TimeUnit.SECONDS,
                            new ArrayBlockingQueue<>(metricsCollectors.size()),
                            taskThreadFactory);
        }

        long prevStartTimestamp = System.currentTimeMillis();

        while (true) {
            try {
                long millisToSleep =
                        minTimeIntervalToSleep - System.currentTimeMillis() + prevStartTimestamp;
                if (millisToSleep > 0) {
                    Thread.sleep(millisToSleep);
                }
            } catch (Exception ex) {
                LOG.error("Exception in Thread Sleep", ex);
            }

            prevStartTimestamp = System.currentTimeMillis();

            if (getEnabled()) {
                long currentTime = System.currentTimeMillis();

                for (Map.Entry<PerformanceAnalyzerMetricsCollector, Long> entry :
                        metricsCollectors.entrySet()) {
                    if (entry.getValue() <= currentTime) {
                        PerformanceAnalyzerMetricsCollector collector = entry.getKey();
                        if (collector.getState()
                                == PerformanceAnalyzerMetricsCollector.State.MUTED) {
                            PerformanceAnalyzerApp.WRITER_METRICS_AGGREGATOR.updateStat(
                                    WriterMetrics.COLLECTORS_MUTED,
                                    collector.getCollectorName(),
                                    1);
                            continue;
                        }
                        metricsCollectors.put(
                                collector, entry.getValue() + collector.getTimeInterval());
                        if (!collector.inProgress()) {
                            collector.setStartTime(currentTime);
                            metricsCollectorsTP.execute(collector);
                        } else {
                            /**
                             * Always run StatsCollector; we rely on StatsCollector for framework
                             * service metrics
                             */
                            if (collector
                                    .getCollectorName()
                                    .equals(StatsCollector.COLLECTOR_NAME)) {
                                LOG.info(
                                        " {} is still in progress; StatsCollector is critical for framework service metrics",
                                        StatsCollector.COLLECTOR_NAME);
                                return;
                            }
                            if (collector.getState()
                                    == PerformanceAnalyzerMetricsCollector.State.HEALTHY) {
                                collector.setState(PerformanceAnalyzerMetricsCollector.State.SLOW);
                                PerformanceAnalyzerApp.WRITER_METRICS_AGGREGATOR.updateStat(
                                        WriterMetrics.COLLECTORS_SLOW,
                                        collector.getCollectorName(),
                                        1);
                            } else if (collector.getState()
                                    == PerformanceAnalyzerMetricsCollector.State.SLOW) {
                                collector.setState(PerformanceAnalyzerMetricsCollector.State.MUTED);
                            }
                            LOG.info(
                                    "Collector {} is still in progress, so skipping this Interval",
                                    collector.getCollectorName());
                            PerformanceAnalyzerApp.WRITER_METRICS_AGGREGATOR.updateStat(
                                    WriterMetrics.COLLECTORS_SKIPPED,
                                    collector.getCollectorName(),
                                    1);
                        }
                    }
                }
            }
        }
    }
}
