/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.metrics;


import java.util.Collections;
import java.util.List;
import org.opensearch.performanceanalyzer.commons.metrics.MeasurementSet;
import org.opensearch.performanceanalyzer.commons.stats.Statistics;

/**
 * metrics added by each RCA vertex in RCA graph. All metrics under this category are RCA specific
 */
public enum RcaVerticesMetrics implements MeasurementSet {
    INVALID_OLD_GEN_SIZE("InvalidOldGenSize", "count", Collections.singletonList(Statistics.COUNT)),

    OLD_GEN_RECLAMATION_INEFFECTIVE(
            "OldGenReclamationIneffective", "count", Collections.singletonList(Statistics.COUNT)),

    OLD_GEN_CONTENDED("OldGenContended", "count", Collections.singletonList(Statistics.COUNT)),

    OLD_GEN_OVER_OCCUPIED(
            "OldGenOverOccupied", "count", Collections.singletonList(Statistics.COUNT)),

    HOT_SHARD_RCA_ERROR("HotShardRcaError", "count", Collections.singletonList(Statistics.COUNT)),

    NUM_YOUNG_GEN_RCA_TRIGGERED(
            "YoungGenRcaCount", "count", Collections.singletonList(Statistics.COUNT)),
    NUM_OLD_GEN_RCA_TRIGGERED(
            "OldGenRcaCount", "count", Collections.singletonList(Statistics.COUNT)),
    NUM_HIGH_HEAP_CLUSTER_RCA_TRIGGERED(
            "HighHeapClusterRcaCount", "count", Collections.singletonList(Statistics.COUNT)),
    YOUNG_GEN_RCA_NAMED_COUNT(
            "YoungGenRcaNamedCount",
            "namedCount",
            Collections.singletonList(Statistics.NAMED_COUNTERS)),
    NUM_FIELD_DATA_CACHE_RCA_TRIGGERED(
            "FieldDataCacheRcaCount", "count", Collections.singletonList(Statistics.COUNT)),
    NUM_SHARD_REQUEST_CACHE_RCA_TRIGGERED(
            "ShardRequestCacheCount", "count", Collections.singletonList(Statistics.COUNT)),
    CLUSTER_RCA_NAMED_COUNT(
            "ClusterRcaNamedCount",
            "namedCount",
            Collections.singletonList(Statistics.NAMED_COUNTERS)),
    ADMISSION_CONTROL_RCA_TRIGGERED(
            "AdmissionControlRcaCount", "count", Collections.singletonList(Statistics.COUNT));
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

    RcaVerticesMetrics(String name, String unit, List<Statistics> statisticList) {
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
