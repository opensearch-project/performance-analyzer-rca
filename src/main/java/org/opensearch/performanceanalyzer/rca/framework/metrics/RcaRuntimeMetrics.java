/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.metrics;


import java.util.Collections;
import java.util.List;
import org.opensearch.performanceanalyzer.rca.stats.eval.Statistics;
import org.opensearch.performanceanalyzer.rca.stats.measurements.MeasurementSet;

public enum RcaRuntimeMetrics implements MeasurementSet {
    /** The number of times the framework was stopped by the operator. */
    RCA_STOPPED_BY_OPERATOR(
            "RcaStoppedByOperator", "count", Collections.singletonList(Statistics.COUNT)),

    /** The number of times the framework was restarted by the operator. */
    RCA_RESTARTED_BY_OPERATOR(
            "RcaRestartedByOperator", "count", Collections.singletonList(Statistics.COUNT)),

    /**
     * OpenSearch APIs calls are expensive and we want to keep track of how many we are making. This
     * is a named counter and therefore we can get a count per OpenSearch API.
     */
    OPEN_SEARCH_APIS_CALLED(
            "OpenSearchApisCalled", "count", Collections.singletonList(Statistics.NAMED_COUNTERS)),

    /**
     * Metric tracking if RCA is enabled or disabled. We write a 0 if RCA is disabled and 1 if it is
     * enabled.
     */
    RCA_ENABLED("RcaEnabled", "count", Collections.singletonList(Statistics.SAMPLE)),

    /** Metric tracking the actions published by the publisher that are persisted in sqlite. */
    NO_INCREASE_ACTION_SUGGESTED(
            "NoIncreaseAction", "namedCount", Collections.singletonList(Statistics.NAMED_COUNTERS)),

    /** Metric tracking the Heap Size increase action published by the publisher. */
    HEAP_SIZE_INCREASE_ACTION_SUGGESTED(
            "HeapSizeIncreaseAction", "count", Collections.singletonList(Statistics.COUNT)),

    /** Metric tracking the actions published by the publisher that are persisted in sqlite. */
    ACTIONS_PUBLISHED(
            "ActionsPublished", "namedCount", Collections.singletonList(Statistics.NAMED_COUNTERS));

    /** What we want to appear as the metric name. */
    private String name;

    /**
     * The unit the measurement is in. This is not used for the statistics calculations but as an
     * information that will be dumped with the metrics.
     */
    private String unit;

    /**
     * Multiple statistics can be collected for each measurement like MAX, MIN and MEAN. This is a
     * collection of one or more such statistics.
     */
    private List<Statistics> statsList;

    RcaRuntimeMetrics(String name, String unit, List<Statistics> statisticList) {
        this.name = name;
        this.unit = unit;
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
    public String getName() {
        return name;
    }

    @Override
    public String getUnit() {
        return unit;
    }
}
