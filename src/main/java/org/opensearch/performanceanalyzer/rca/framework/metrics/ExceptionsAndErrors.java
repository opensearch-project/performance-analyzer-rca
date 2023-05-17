/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.metrics;


import java.util.Collections;
import java.util.List;
import org.opensearch.performanceanalyzer.rca.stats.eval.Statistics;
import org.opensearch.performanceanalyzer.rca.stats.measurements.MeasurementSet;

public enum ExceptionsAndErrors implements MeasurementSet {
    INVALID_CONFIG_RCA_AGENT_STOPPED("InvalidConfigRCAAgentStopped"),
    RCA_FRAMEWORK_CRASH("RcaFrameworkCrash"),
    CONFIG_DIR_NOT_FOUND("ConfigDirectoryNotFound"),
    WRITE_UPDATED_RCA_CONF_ERROR("WriteUpdatedRcaConfError"),
    BATCH_METRICS_CONFIG_ERROR("BatchMetricsConfigError"),
    MUTE_ERROR("MuteError"),

    /**
     * Aggregate metrics across RCA Graph nodes tracking failure in various staged of RCA Graph
     * Execution.
     */
    /** Exception thrown in operate() method, implemented by each RCA Graph node */
    EXCEPTION_IN_OPERATE("ExceptionInOperate", "namedCount", Statistics.NAMED_COUNTERS),

    /** Exception thrown in compute() method in publisher. */
    EXCEPTION_IN_COMPUTE("ExceptionInCompute", "namedCount", Statistics.NAMED_COUNTERS),

    /** When calling the MetricsDB API throws an exception. */
    EXCEPTION_IN_GATHER("ExceptionInGather", "namedCount", Statistics.NAMED_COUNTERS),

    /** Exception thrown when persisting action or flowunit when unable to write to DB */
    EXCEPTION_IN_PERSIST("ExceptionInPersist", "namedCount", Statistics.NAMED_COUNTERS),

    /** Metrics tracking PA Plugin level: 1. Errors 2. Exceptions */
    /** Tracks stale metrics - metrics to be collected is behind current bucket */
    STALE_METRICS("StaleMetrics"),

    /** Tracks the number of VM attach/dataDump or detach failures. */
    JVM_ATTACH_ERROR("JvmAttachErrror"),

    /** java_pid file is missing. */
    JVM_ATTACH_ERROR_JAVA_PID_FILE_MISSING("JvmAttachErrorJavaPidFileMissing"),

    /** Lock could not be acquired within the timeout. */
    JVM_ATTACH_LOCK_ACQUISITION_FAILED("JvmAttachLockAcquisitionFailed"),

    /** ThreadState could not be found for an OpenSearch thread in the critical OpenSearch path. */
    NO_THREAD_STATE_INFO("NoThreadStateInfo"),

    /** Thread ID is no loner exists */
    JVM_THREAD_ID_NO_LONGER_EXISTS("JVMThreadIdNoLongerExists"),

    /** This metric indicates failure in collecting ClusterManagerServiceEventMetrics */
    CLUSTER_MANAGER_METRICS_ERROR("ClusterManagerMetricsError"),

    /** This metric indicates cluster_manager is not up */
    CLUSTER_MANAGER_NODE_NOT_UP("ClusterManagerNodeNotUp"),

    /** This metric indicates faiure in intercepting opensearch requests at transport channel */
    OPENSEARCH_REQUEST_INTERCEPTOR_ERROR("OpenSearchRequestInterceptorError"),

    /** This metric indicates metric entry insertion to event log queue failed */
    METRICS_WRITE_ERROR("MetricsWriteError", "namedCount", Statistics.NAMED_COUNTERS),

    /** This metric indicates faiure in cleaning up the event log files */
    METRICS_REMOVE_ERROR("MetricsRemoveError", "namedCount", Statistics.NAMED_COUNTERS),

    /** This metric indicates faiure in cleaning up the event log files */
    METRICS_REMOVE_FAILURE("MetricsRemoveFailure", "namedCount", Statistics.NAMED_COUNTERS),

    /** This metric indicates that the writer file creation was skipped. */
    WRITER_FILE_CREATION_SKIPPED(
            "WriterFileCreationSkipped", "namedCount", Statistics.NAMED_COUNTERS),

    /** This metric indicates that error occurred while closing grpc channels. */
    GRPC_CHANNEL_CLOSURE_ERROR("GrpcChannelClosureError", "namedCount", Statistics.NAMED_COUNTERS),

    /** This metric indicates that error occurred while closing grpc server. */
    GRPC_SERVER_CLOSURE_ERROR("GrpcServerClosureError", "namedCount", Statistics.NAMED_COUNTERS),

    /** This metric indicates that error occurred while closing metrics db. */
    METRICS_DB_CLOSURE_ERROR("MetricsDbClosureError", "namedCount", Statistics.NAMED_COUNTERS),

    /** This metric indicates that error occurred while closing database connection. */
    IN_MEMORY_DATABASE_CONN_CLOSURE_ERROR(
            "InMemoryDatabaseConnClosureError", "namedCount", Statistics.NAMED_COUNTERS),

    /** Batch Metric relevant errors */
    BATCH_METRICS_HTTP_CLIENT_ERROR("BatchMetricsHttpClientError"),
    BATCH_METRICS_HTTP_HOST_ERROR("BatchMetricsHttpHostError"),
    BATCH_METRICS_EXCEEDED_MAX_DATAPOINTS("ExceededBatchMetricsMaxDatapoints"),

    /** When the reader encounters errors accessing metricsdb files. */
    READER_METRICSDB_ACCESS_ERRORS("ReaderMetricsdbAccessError"),
    SHARD_STATE_COLLECTOR_ERROR("ShardStateCollectorError"),
    ADMISSION_CONTROL_COLLECTOR_ERROR("AdmissionControlCollectorError"),
    CIRCUIT_BREAKER_COLLECTOR_ERROR("CircuitBreakerCollectorError"),
    CLUSTER_MANAGER_THROTTLING_COLLECTOR_ERROR("ClusterManagerThrottlingMetricsCollectorError"),
    FAULT_DETECTION_COLLECTOR_ERROR("FaultDetectionMetricsCollectorError"),
    CLUSTER_APPLIER_SERVICE_STATS_COLLECTOR_ERROR("ClusterApplierServiceStatsCollectorError"),
    ELECTION_TERM_COLLECTOR_ERROR("ElectionTermCollectorError"),
    SHARD_INDEXING_PRESSURE_COLLECTOR_ERROR("ShardIndexingPressureMetricsCollectorError"),
    NODESTATS_COLLECTION_ERROR("NodeStatsCollectionError");

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

    ExceptionsAndErrors(String name) {
        this.name = name;
        this.unit = "count";
        this.statsList = Collections.singletonList(Statistics.COUNT);
    }

    ExceptionsAndErrors(String name, String unit, Statistics stats) {
        this.name = name;
        this.unit = unit;
        this.statsList = Collections.singletonList(stats);
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
