/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.metrics;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.opensearch.performanceanalyzer.rca.stats.eval.Statistics;
import org.opensearch.performanceanalyzer.rca.stats.measurements.MeasurementSet;

public enum WriterMetrics implements MeasurementSet {
    /** Measures the time spent in deleting the event log files */
    EVENT_LOG_FILES_DELETION_TIME(
            "EventLogFilesDeletionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    /** Measures the count of event log files deleted */
    EVENT_LOG_FILES_DELETED(
            "EventLogFilesDeleted", "count", Arrays.asList(Statistics.MAX, Statistics.SUM)),

    /**
     * Successfully completed a thread-dump. An omission of indicate thread taking the dump got
     * stuck.
     */
    JVM_THREAD_DUMP_SUCCESSFUL("JvmThreadDumpSuccessful"),

    /** Tracks the number of muted collectors */
    COLLECTORS_MUTED(
            "CollectorsMutedCount",
            "namedCount",
            Collections.singletonList(Statistics.NAMED_COUNTERS)),

    /** Tracks time taken by respective collectors to collect event metrics. */
    THREADPOOL_METRICS_COLLECTOR_EXECUTION_TIME(
            "ThreadPoolMetricsCollectorExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    CACHE_CONFIG_METRICS_COLLECTOR_EXECUTION_TIME(
            "CacheConfigMetricsCollectorExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    CIRCUIT_BREAKER_COLLECTOR_EXECUTION_TIME(
            "CircuitBreakerCollectorExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    OS_METRICS_COLLECTOR_EXECUTION_TIME(
            "OSMetricsCollectorExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    HEAP_METRICS_COLLECTOR_EXECUTION_TIME(
            "HeapMetricsCollectorExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    NODE_DETAILS_COLLECTOR_EXECUTION_TIME(
            "NodeDetailsCollectorExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    NODE_STATS_ALL_SHARDS_METRICS_COLLECTOR_EXECUTION_TIME(
            "NodeStatsAllShardsMetricsCollectorExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    NODE_STATS_FIXED_SHARDS_METRICS_COLLECTOR_EXECUTION_TIME(
            "NodeStatsFixedShardsMetricsCollectorExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    CLUSTER_MANAGER_SERVICE_METRICS_COLLECTOR_EXECUTION_TIME(
            "ClusterManagerServiceMetricsCollectorExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    CLUSTER_MANAGER_SERVICE_EVENTS_METRICS_COLLECTOR_EXECUTION_TIME(
            "ClusterManagerServiceEventsMetricsCollectorExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    NETWORK_INTERFACE_COLLECTOR_EXECUTION_TIME(
            "NetworkInterfaceCollectorExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    GC_INFO_COLLECTOR_EXECUTION_TIME(
            "GCInfoCollectorExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    FAULT_DETECTION_COLLECTOR_EXECUTION_TIME(
            "FaultDetectionCollectorExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    SHARD_STATE_COLLECTOR_EXECUTION_TIME(
            "ShardStateCollectorExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    CLUSTER_MANAGER_THROTTLING_COLLECTOR_EXECUTION_TIME(
            "ClusterManagerThrottlingCollectorExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    CLUSTER_APPLIER_SERVICE_STATS_COLLECTOR_EXECUTION_TIME(
            "ClusterApplierServiceStatsCollectorExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    ADMISSION_CONTROL_METRICS_COLLECTOR_EXECUTION_TIME(
            "AdmissionControlMetricsCollectorExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    ELECTION_TERM_COLLECTOR_EXECUTION_TIME(
            "ElectionTermCollectorExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    SHARD_INDEXING_PRESSURE_COLLECTOR_EXECUTION_TIME(
            "ShardIndexingPressureCollectorExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),

    /** Tracks collector specific metrics - available/enabled/disabled and other params */
    ADMISSION_CONTROL_COLLECTOR_NOT_AVAILABLE(
            "AdmissionControlCollectorNotAvailable",
            "count",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),

    CLUSTER_MANAGER_CLUSTER_UPDATE_STATS_COLLECTOR_DISABLED(
            "ClusterManagerClusterUpdateStatsCollectorDisabled",
            "count",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),

    CLUSTER_MANAGER_THROTTLING_COLLECTOR_NOT_AVAILABLE(
            "ClusterManagerThrottlingCollectorNotAvailable",
            "count",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM));

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

    WriterMetrics(String name) {
        this.name = name;
        this.unit = "count";
        this.statsList = Collections.singletonList(Statistics.COUNT);
    }

    WriterMetrics(String name, String unit, List<Statistics> stats) {
        this.name = name;
        this.unit = unit;
        this.statsList = stats;
    }

    WriterMetrics(String name, String unit, Statistics stats) {
        this(name, unit, Collections.singletonList(stats));
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
