/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.metrics;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.opensearch.performanceanalyzer.commons.stats.eval.Statistics;
import org.opensearch.performanceanalyzer.commons.stats.measurements.MeasurementSet;
import org.opensearch.performanceanalyzer.commons.stats.metrics.StatsType;

public enum ReaderMetrics implements MeasurementSet {

    /**
     * Tracks MetricDB related metrics, the size of below metricDB file is factor of events(shard +
     * cluster traffic) and also the collector functioning.
     */
    METRICSDB_FILE_SIZE(
            "MetricsdbFileSize",
            "bytes",
            StatsType.STATS_DATA,
            Arrays.asList(Statistics.MAX, Statistics.MEAN)),
    METRICSDB_NUM_FILES("MetricsdbNumFiles", "count", StatsType.STATS_DATA, Statistics.SAMPLE),
    METRICSDB_SIZE_FILES("MetricsdbSizeFiles", "bytes", StatsType.STATS_DATA, Statistics.SAMPLE),
    METRICSDB_NUM_UNCOMPRESSED_FILES(
            "MetricsdbNumUncompressedFiles", "count", StatsType.STATS_DATA, Statistics.SAMPLE),
    METRICSDB_SIZE_UNCOMPRESSED_FILES(
            "MetricsdbSizeUncompressedFiles", "bytes", StatsType.STATS_DATA, Statistics.SAMPLE),
    BATCH_METRICS_ENABLED("BatchMetricsEnabled", "count", StatsType.STATS_DATA, Statistics.SAMPLE),
    BATCH_METRICS_HTTP_SUCCESS("BatchMetricsHttpSuccess"),
    BATCH_METRICS_QUERY_PROCESSING_TIME(
            "BatchMetricsQueryProcessingTime", "millis", StatsType.LATENCIES, Statistics.SUM),

    /**
     * Tracks time taken by respective emitters and the total time to process and emit event
     * metrics.
     */
    READER_METRICS_EMIT_TIME(
            "ReaderMetricsEmitTime", "millis", StatsType.LATENCIES, Statistics.SUM),
    READER_OS_METRICS_EMIT_TIME(
            "ReaderOSMetricsEmitTime", "millis", StatsType.LATENCIES, Statistics.SUM),
    READER_METRICS_PROCESS_TIME(
            "ReaderMetricsProcessTime", "millis", StatsType.LATENCIES, Statistics.SUM),
    GC_INFO_EMITTER_EXECUTION_TIME(
            "GCInfoEmitterExecutionTime", "millis", StatsType.LATENCIES, Statistics.SUM),
    WORKLOAD_METRICS_EMITTER_EXECUTION_TIME(
            "WorkloadMetricsEmitterExecutionTime", "millis", StatsType.LATENCIES, Statistics.SUM),
    THREAD_NAME_METRICS_EMITTER_EXECUTION_TIME(
            "ThreadNameMetricsEmitterExecutionTime", "millis", StatsType.LATENCIES, Statistics.SUM),
    AGGREGATED_OS_METRICS_EMITTER_EXECUTION_TIME(
            "AggregatedOSMetricsEmitterExecutionTime",
            "millis",
            StatsType.LATENCIES,
            Statistics.SUM),
    SHARD_REQUEST_METRICS_EMITTER_EXECUTION_TIME(
            "ShardRequestMetricsEmitterExecutionTime",
            "millis",
            StatsType.LATENCIES,
            Statistics.SUM),
    HTTP_METRICS_EMITTER_EXECUTION_TIME(
            "HTTPMetricsEmitterExecutionTime", "millis", StatsType.LATENCIES, Statistics.SUM),
    ADMISSION_CONTROL_METRICS_EMITTER_EXECUTION_TIME(
            "AdmissionControlMetricsEmitterExecutionTime",
            "millis",
            StatsType.LATENCIES,
            Statistics.SUM),
    CLUSTER_MANAGER_EVENT_METRICS_EMITTER_EXECUTION_TIME(
            "ClusterManagerEventMetricsEmitterExecutionTime",
            "millis",
            StatsType.LATENCIES,
            Statistics.SUM),
    NODE_METRICS_EMITTER_EXECUTION_TIME(
            "NodeMetricsEmitterExecutionTime", "millis", StatsType.LATENCIES, Statistics.SUM),
    SHARD_STATE_EMITTER_EXECUTION_TIME(
            "ShardStateEmitterExecutionTime", "millis", StatsType.LATENCIES, Statistics.SUM),
    CLUSTER_MANAGER_THROTTLING_EMITTER_EXECUTION_TIME(
            "ClusterManagerThrottlingEmitterExecutionTime",
            "millis",
            StatsType.LATENCIES,
            Statistics.SUM),
    FAULT_DETECTION_METRICS_EMITTER_EXECUTION_TIME(
            "FaultDetectionMetricsEmitterExecutionTime",
            "millis",
            StatsType.LATENCIES,
            Statistics.SUM),
    SEARCH_BACK_PRESSURE_METRICS_EMITTER_EXECUTION_TIME(
            "SearchBackPressureMetricsEmitterExecutionTime",
            "millis",
            StatsType.LATENCIES,
            Statistics.SUM),
    ;

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

    ReaderMetrics(String name) {
        this(name, "count", StatsType.STATS_DATA, Collections.singletonList(Statistics.COUNT));
    }

    ReaderMetrics(String name, String unit, StatsType statType, Statistics stats) {
        this(name, unit, statType, Collections.singletonList(stats));
    }

    ReaderMetrics(String name, String unit, StatsType statType, List<Statistics> stats) {
        this.name = name;
        this.unit = unit;
        this.statsType = statType;
        this.statsList = stats;
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
