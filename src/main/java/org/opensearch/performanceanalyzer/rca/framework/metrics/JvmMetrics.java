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

public enum JvmMetrics implements MeasurementSet {
    JVM_FREE_MEM_SAMPLER("JvmFreeMem", "bytes"),
    JVM_TOTAL_MEM_SAMPLER("JvmTotalMem", "bytes"),
    THREAD_COUNT("ThreadCount", "count");

    private String name;
    private String unit;
    private StatsType statsType;

    JvmMetrics(String name, String unit) {
        this.name = name;
        this.unit = unit;
        this.statsType = StatsType.STATS_DATA;
    }

    @Override
    public List<Statistics> getStatsList() {
        return Collections.singletonList(Statistics.SAMPLE);
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

    @Override
    public String toString() {
        return new StringBuilder(name).append("-").append(unit).toString();
    }
}
