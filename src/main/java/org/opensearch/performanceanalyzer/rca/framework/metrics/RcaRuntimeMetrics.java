/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.metrics;


import java.util.Collections;
import java.util.List;
import org.opensearch.performanceanalyzer.commons.stats.eval.Statistics;
import org.opensearch.performanceanalyzer.commons.stats.measurements.MeasurementSet;
import org.opensearch.performanceanalyzer.commons.stats.metrics.StatsType;
import org.opensearch.performanceanalyzer.rca.RcaController;

public enum RcaRuntimeMetrics implements MeasurementSet {
    /** Tracks scheduler restart issued at {@link RcaController#restart()} */
    RCA_SCHEDULER_RESTART("RcaSchedulerRestart"),

    /** The number of times the framework was stopped by the operator. */
    RCA_STOPPED_BY_OPERATOR("RcaStoppedByOperator"),

    /** The number of times the framework was restarted by the operator. */
    RCA_RESTARTED_BY_OPERATOR("RcaRestartedByOperator"),

    /**
     * OpenSearch APIs calls are expensive and we want to keep track of how many we are making. This
     * is a named counter and therefore we can get a count per OpenSearch API.
     */
    OPEN_SEARCH_APIS_CALLED(
            "OpenSearchApisCalled", "namedCount", StatsType.STATS_DATA, Statistics.NAMED_COUNTERS),

    /**
     * Metric tracking if RCA is enabled or disabled. We write a 0 if RCA is disabled and 1 if it is
     * enabled.
     */
    RCA_ENABLED("RcaEnabled", "count", StatsType.STATS_DATA, Statistics.SAMPLE),

    /** Metric tracking the actions published by the publisher that are persisted in sqlite. */
    NO_INCREASE_ACTION_SUGGESTED(
            "NoIncreaseAction", "namedCount", StatsType.STATS_DATA, Statistics.NAMED_COUNTERS),

    /** Metric tracking the Heap Size increase action published by the publisher. */
    HEAP_SIZE_INCREASE_ACTION_SUGGESTED("HeapSizeIncreaseAction"),

    /** Metric tracking the actions published by the publisher that are persisted in sqlite. */
    ACTIONS_PUBLISHED(
            "ActionsPublished", "namedCount", StatsType.STATS_DATA, Statistics.NAMED_COUNTERS),

    /**
     * Tracks transport thread state(WAITING, TIMED-WAITING, BLOCKED) and time. T The last 2 metrics
     * track time for 60s moving window.
     */
    BLOCKED_TRANSPORT_THREAD_COUNT(
            "BlockedTransportThreadCount", "count", StatsType.STATS_DATA, Statistics.MAX),
    WAITED_TRANSPORT_THREAD_COUNT(
            "WaitedTransportThreadCount", "count", StatsType.STATS_DATA, Statistics.MAX),
    MAX_TRANSPORT_THREAD_BLOCKED_TIME(
            "MaxTransportThreadBlockedTime", "seconds", StatsType.LATENCIES, Statistics.MAX),
    MAX_TRANSPORT_THREAD_WAITED_TIME(
            "MaxTransportThreadWaitedTime", "seconds", StatsType.LATENCIES, Statistics.MAX);

    /** What we want to appear as the metric name. */
    private String name;

    /**
     * The unit the measurement is in. This is not used for the statistics calculations but as an
     * information that will be dumped with the metrics.
     */
    private String unit;

    /** The type of the measurement, refer {@link StatsType} */
    private StatsType statsType;

    /**
     * Multiple statistics can be collected for each measurement like MAX, MIN and MEAN. This is a
     * collection of one or more such statistics.
     */
    private List<Statistics> statsList;

    RcaRuntimeMetrics(String name) {
        this(name, "count", StatsType.STATS_DATA, Collections.singletonList(Statistics.COUNT));
    }

    RcaRuntimeMetrics(String name, String unit, StatsType statsType, Statistics stats) {
        this(name, unit, statsType, Collections.singletonList(stats));
    }

    RcaRuntimeMetrics(
            String name, String unit, StatsType statsType, List<Statistics> statisticList) {
        this.name = name;
        this.unit = unit;
        this.statsType = statsType;
        this.statsList = statisticList;
    }

    public String toString() {
        return new StringBuilder(name).append("-").append(unit).toString();
    }

    @Override
    public List<Statistics> getStatsList() {
        return statsList;
    }

    @Override
    public StatsType getStatsType() {
        return statsType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUnit() {
        return unit;
    }
}
