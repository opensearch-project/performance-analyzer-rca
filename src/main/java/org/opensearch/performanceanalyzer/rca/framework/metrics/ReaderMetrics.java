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

public enum ReaderMetrics implements MeasurementSet {

    /**
     * We start 6 threads within RCA Agent, details at {@link
     * org.opensearch.performanceanalyzer.PerformanceAnalyzerThreads}. Below metrics track count of
     * thread started and ended.
     */
    NUM_PA_THREADS_STARTED(
            "NumberOfPAThreadsStarted", "namedCount", Collections.singletonList(Statistics.COUNT)),

    NUM_PA_THREADS_ENDED(
            "NumberOfPAThreadsEnded", "namedCount", Collections.singletonList(Statistics.COUNT)),

    /**
     * For each thread defined in {@link
     * org.opensearch.performanceanalyzer.PerformanceAnalyzerThreads}, we add a respective
     * 'threadExceptionCode' metric. These metrics are emitted in catch block of {@link
     * org.opensearch.performanceanalyzer.threads.ThreadProvider#createThreadForRunnable}
     */
    READER_THREAD_STOPPED(
            "ReaderThreadStopped", "count", Collections.singletonList(Statistics.COUNT)),

    ERROR_HANDLER_THREAD_STOPPED(
            "ErrorHandlerThreadStopped", "count", Collections.singletonList(Statistics.COUNT)),

    GRPC_SERVER_THREAD_STOPPED(
            "GRPCServerThreadStopped", "count", Collections.singletonList(Statistics.COUNT)),

    WEB_SERVER_THREAD_STOPPED(
            "WebServerThreadStopped", "count", Collections.singletonList(Statistics.COUNT)),

    RCA_CONTROLLER_THREAD_STOPPED(
            "RcaControllerThreadStopped", "count", Collections.singletonList(Statistics.COUNT)),

    RCA_SCHEDULER_THREAD_STOPPED(
            "RcaSchedulerThreadStopped", "count", Collections.singletonList(Statistics.COUNT)),

    /** Tracks time taken by Reader thread to emit event metrics . */
    READER_METRICS_EMIT_TIME(
            "ReaderMetricsEmitTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),

    /**
     * Tracks scheduler restart issued at {@link
     * org.opensearch.performanceanalyzer.rca.RcaController#restart}
     */
    RCA_SCHEDULER_RESTART(
            "RcaSchedulerRestart", "count", Collections.singletonList(Statistics.COUNT)),

    /** Size of generated metricsdb files. */
    METRICSDB_FILE_SIZE(
            "MetricsdbFileSize", "bytes", Arrays.asList(Statistics.MAX, Statistics.MEAN)),

    /** Number of compressed and uncompressed metricsdb files. */
    METRICSDB_NUM_FILES("MetricsdbNumFiles", "count", Statistics.SAMPLE),

    /** Size of compressed and uncompressed metricsdb files. */
    METRICSDB_SIZE_FILES("MetricsdbSizeFiles", "bytes", Statistics.SAMPLE),

    /** Number of uncompressed metricsdb files. */
    METRICSDB_NUM_UNCOMPRESSED_FILES("MetricsdbNumUncompressedFiles", "count", Statistics.SAMPLE),

    /** Size of uncompressed metricsdb files. */
    METRICSDB_SIZE_UNCOMPRESSED_FILES("MetricsdbSizeUncompressedFiles", "bytes", Statistics.SAMPLE),

    /** Whether or not batch metrics is enabled (0 for enabled, 1 for disabled). */
    BATCH_METRICS_ENABLED("BatchMetricsEnabled", "count", Statistics.SAMPLE),

    /** Number of http requests where the client gave a bad request. */
    BATCH_METRICS_HTTP_CLIENT_ERROR("BatchMetricsHttpClientError", "count", Statistics.COUNT),

    /** Number of http requests where the host could not generate a correct response. */
    BATCH_METRICS_HTTP_HOST_ERROR("BatchMetricsHttpHostError", "count", Statistics.COUNT),

    /** Number of successful queries. */
    BATCH_METRICS_HTTP_SUCCESS("BatchMetricsHttpSuccess", "count", Statistics.COUNT),

    /**
     * Number of times a query for batch metrics exceeded the maximum number of requestable
     * datapoints.
     */
    BATCH_METRICS_EXCEEDED_MAX_DATAPOINTS(
            "ExceededBatchMetricsMaxDatapoints", "count", Statistics.COUNT),

    /** Amount of time required to process valid batch metrics requests. */
    BATCH_METRICS_QUERY_PROCESSING_TIME(
            "BatchMetricsQueryProcessingTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),

    /** Amount of time taken to emit Shard State metrics. */
    SHARD_STATE_EMITTER_EXECUTION_TIME(
            "ShardStateEmitterExecutionTime",
            "millis",
            Arrays.asList(
                    Statistics.MAX,
                    Statistics.MIN,
                    Statistics.MEAN,
                    Statistics.COUNT,
                    Statistics.SUM)),

    /** Amount of time taken to emit Master throttling metrics. */
    MASTER_THROTTLING_EMITTER_EXECUTION_TIME(
            "MasterThrottlingEmitterExecutionTime",
            "millis",
            Arrays.asList(
                    Statistics.MAX,
                    Statistics.MIN,
                    Statistics.MEAN,
                    Statistics.COUNT,
                    Statistics.SUM)),

    FAULT_DETECTION_METRICS_EMITTER_EXECUTION_TIME(
            "FaultDetectionMetricsEmitterExecutionTime",
            "millis",
            Arrays.asList(
                    Statistics.MAX,
                    Statistics.MIN,
                    Statistics.MEAN,
                    Statistics.COUNT,
                    Statistics.SUM)),

    /**
     * A blanket exception code for {@link
     * org.opensearch.performanceanalyzer.reader.ReaderMetricsProcessor} failures.
     */
    OTHER("Other", "count", Collections.singletonList(Statistics.COUNT));

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

    ReaderMetrics(String name, String unit, List<Statistics> stats) {
        this.name = name;
        this.unit = unit;
        this.statsList = stats;
    }

    ReaderMetrics(String name, String unit, Statistics stats) {
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
