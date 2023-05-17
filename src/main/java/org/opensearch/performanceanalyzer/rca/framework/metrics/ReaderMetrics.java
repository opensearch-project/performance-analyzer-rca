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
     *
     * <p>Note: The 'PA' in metricName is confusing, it is meant to imply threads started within RCA
     * Agent.
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

    /**
     * Tracks MetricDB related metrics, the size of below metricDB file is factor of events(shard +
     * cluster traffic) and also the collector functioning.
     */
    METRICSDB_FILE_SIZE(
            "MetricsdbFileSize", "bytes", Arrays.asList(Statistics.MAX, Statistics.MEAN)),
    METRICSDB_NUM_FILES("MetricsdbNumFiles", "count", Statistics.SAMPLE),
    METRICSDB_SIZE_FILES("MetricsdbSizeFiles", "bytes", Statistics.SAMPLE),
    METRICSDB_NUM_UNCOMPRESSED_FILES("MetricsdbNumUncompressedFiles", "count", Statistics.SAMPLE),
    METRICSDB_SIZE_UNCOMPRESSED_FILES("MetricsdbSizeUncompressedFiles", "bytes", Statistics.SAMPLE),
    BATCH_METRICS_ENABLED("BatchMetricsEnabled", "count", Statistics.SAMPLE),
    BATCH_METRICS_HTTP_SUCCESS("BatchMetricsHttpSuccess", "count", Statistics.COUNT),
    BATCH_METRICS_QUERY_PROCESSING_TIME(
            "BatchMetricsQueryProcessingTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),

    /**
     * Tracks time taken by respective emitters and the total time to process and emit event
     * metrics.
     */
    READER_METRICS_EMIT_TIME(
            "ReaderMetricsEmitTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    READER_METRICS_PROCESS_TIME(
            "ReaderMetricsProcessTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    GC_INFO_EMITTER_EXECUTION_TIME(
            "GCInfoEmitterExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    WORKLOAD_METRICS_EMITTER_EXECUTION_TIME(
            "WorkloadMetricsEmitterExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    THREAD_NAME_METRICS_EMITTER_EXECUTION_TIME(
            "ThreadNameMetricsEmitterExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    AGGREGATED_OS_METRICS_EMITTER_EXECUTION_TIME(
            "AggregatedOSMetricsEmitterExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    SHARD_REQUEST_METRICS_EMITTER_EXECUTION_TIME(
            "ShardRequestMetricsEmitterExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    HTTP_METRICS_EMITTER_EXECUTION_TIME(
            "HTTPMetricsEmitterExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    ADMISSION_CONTROL_METRICS_EMITTER_EXECUTION_TIME(
            "AdmissionControlMetricsEmitterExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    CLUSTER_MANAGER_EVENT_METRICS_EMITTER_EXECUTION_TIME(
            "ClusterManagerEventMetricsEmitterExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    NODE_METRICS_EMITTER_EXECUTION_TIME(
            "NodeMetricsEmitterExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    SHARD_STATE_EMITTER_EXECUTION_TIME(
            "ShardStateEmitterExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    CLUSTER_MANAGER_THROTTLING_EMITTER_EXECUTION_TIME(
            "ClusterManagerThrottlingEmitterExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),
    FAULT_DETECTION_METRICS_EMITTER_EXECUTION_TIME(
            "FaultDetectionMetricsEmitterExecutionTime",
            "millis",
            Arrays.asList(Statistics.MAX, Statistics.MEAN, Statistics.SUM)),

    /** Number of transport threads in BLOCKED state. */
    BLOCKED_TRANSPORT_THREAD_COUNT("BlockedTransportThreadCount", "count", Statistics.MAX),

    /** Number of transport threads in WAITING or TIMED-WAITING state. */
    WAITED_TRANSPORT_THREAD_COUNT("WaitedTransportThreadCount", "count", Statistics.MAX),

    /** Max amount of time a transport thread has been BLOCKED in the past 60 seconds. */
    MAX_TRANSPORT_THREAD_BLOCKED_TIME("MaxTransportThreadBlockedTime", "seconds", Statistics.MAX),

    /**
     * Max amount of time a transport thread has been in WAITING or TIMED-WAITING state in the past
     * 60 seconds.
     */
    MAX_TRANSPORT_THREAD_WAITED_TIME("MaxTransportThreadWaitedTime", "seconds", Statistics.MAX),

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
