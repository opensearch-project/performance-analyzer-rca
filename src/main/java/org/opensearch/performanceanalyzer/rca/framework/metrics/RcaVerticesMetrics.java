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

/**
 * metrics added by each RCA vertex in RCA graph. All metrics under this category are RCA specific
 */
public enum RcaVerticesMetrics implements MeasurementSet {
    INVALID_OLD_GEN_SIZE("InvalidOldGenSize"),

    OLD_GEN_RECLAMATION_INEFFECTIVE("OldGenReclamationIneffective"),
    OLD_GEN_CONTENDED("OldGenContended"),
    OLD_GEN_OVER_OCCUPIED("OldGenOverOccupied"),
    NUM_YOUNG_GEN_RCA_TRIGGERED("YoungGenRcaCount"),
    NUM_OLD_GEN_RCA_TRIGGERED("OldGenRcaCount"),
    NUM_HIGH_HEAP_CLUSTER_RCA_TRIGGERED("HighHeapClusterRcaCount"),
    YOUNG_GEN_RCA_NAMED_COUNT(
            "YoungGenRcaNamedCount",
            "namedCount",
            StatsType.STATS_DATA,
            Collections.singletonList(Statistics.NAMED_COUNTERS)),
    NUM_FIELD_DATA_CACHE_RCA_TRIGGERED("FieldDataCacheRcaCount"),
    NUM_SHARD_REQUEST_CACHE_RCA_TRIGGERED("ShardRequestCacheCount"),

    HOT_SHARD_RCA_ERROR("HotShardRcaError"),
    ADMISSION_CONTROL_RCA_TRIGGERED("AdmissionControlRcaCount"),

    CLUSTER_RCA_NAMED_COUNT(
            "ClusterRcaNamedCount",
            "namedCount",
            StatsType.STATS_DATA,
            Collections.singletonList(Statistics.NAMED_COUNTERS));

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

    RcaVerticesMetrics(String name) {
        this(name, "count", StatsType.STATS_DATA, Collections.singletonList(Statistics.COUNT));
    }

    RcaVerticesMetrics(
            String name, String unit, StatsType statType, List<Statistics> statisticList) {
        this.name = name;
        this.unit = unit;
        this.statsType = statType;
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
