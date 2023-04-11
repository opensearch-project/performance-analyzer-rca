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
    PA_AGENT_STOPPED("PaAgentStopped"),
    RCA_FRAMEWORK_CRASH("RcaFrameworkCrash"),

    CONFIG_DIR_NOT_FOUND("ConfigDirectoryNotFound"),

    WRITE_UPDATED_RCA_CONF_ERROR("WriteUpdatedRcaConfError"),

    MUTE_ERROR("MuteError"),

    /**
     * These are the cases when an exception was throws in the {@code operate()} method, that each
     * RCA graph node implements.
     */
    EXCEPTION_IN_OPERATE("ExceptionInOperate", "namedCount", Statistics.NAMED_COUNTERS),

    /**
     * These are the cases when an exception was throws in the {@code compute()} method in
     * publisher.
     */
    EXCEPTION_IN_COMPUTE("ExceptionInCompute", "namedCount", Statistics.NAMED_COUNTERS),

    /** When calling the MetricsDB API throws an exception. */
    EXCEPTION_IN_GATHER("ExceptionInGather", "namedCount", Statistics.NAMED_COUNTERS),

    /**
     * When persisting action or flowunits, the persistable throws an exception when it is unable to
     * write to DB.
     */
    EXCEPTION_IN_PERSIST("ExceptionInPersist", "namedCount", Statistics.NAMED_COUNTERS),

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
