/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metrics;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.config.PluginSettings;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ExceptionsAndErrors;
import org.opensearch.performanceanalyzer.reader_writer_shared.Event;

@SuppressWarnings("checkstyle:constantname")
public class PerformanceAnalyzerMetrics {
    private static final Logger LOG = LogManager.getLogger(PerformanceAnalyzerMetrics.class);
    public static final String sDevShmScratchLocation = "performanceanalyzer_scratch";
    public static final String sIndicesPath = "indices";
    public static final String sThreadPoolPath = "thread_pool";
    public static final String sThreadsPath = "threads";
    public static final String sCacheConfigPath = "cache_config";
    public static final String sCircuitBreakerPath = "circuit_breaker";
    public static final String sShardBulkPath = "shardbulk";
    public static final String sShardFetchPath = "shardfetch";
    public static final String sShardQueryPath = "shardquery";
    public static final String sClusterManagerTaskPath = "cluster_manager_task";
    public static final String sFaultDetection = "fault_detection";
    public static final String sClusterApplierService = "cluster_applier_service";
    public static final String sClusterManagerClusterUpdate = "cluster_manager_cluster_update";
    public static final String sElectionTermPath = "election_term";
    public static final String sHttpPath = "http";
    public static final String sOSPath = "os_metrics";
    public static final String sHeapPath = "heap_metrics";
    public static final String sNodesPath = "node_metrics";
    public static final String sPendingTasksPath = "pending_tasks";
    public static final String sClusterManagerThrottledTasksPath =
            "cluster_manager_throttling_metrics";
    public static final String sShardStatePath = "shard_state_metrics";
    public static final String sDisksPath = "disk_metrics";
    public static final String sTCPPath = "tcp_metrics";
    public static final String sIPPath = "ip_metrics";
    public static final String sGcInfoPath = "gc_info";
    public static final String sMountedPartitionMetricsPath = "mounted_part_space";
    public static final String sAdmissionControlMetricsPath = "admission_control_metrics";
    public static final String sShardIndexingPressurePath = "shard_indexing_pressure_metrics";
    public static final String sKeyValueDelimitor = ":";
    public static final String sMetricNewLineDelimitor = System.getProperty("line.separator");
    public static final String START_FILE_NAME = "start";
    public static final String FINISH_FILE_NAME = "finish";
    public static final String CLUSTER_MANAGER_CURRENT = "current";
    public static final String CLUSTER_MANAGER_META_DATA = "metadata";
    public static final String METRIC_CURRENT_TIME = "current_time";
    public static final String FAULT_DETECTION_FOLLOWER_CHECK = "follower_check";
    public static final String FAULT_DETECTION_LEADER_CHECK = "leader_check";
    public static final String FAULT = "fault";
    public static final int QUEUE_SIZE = PluginSettings.instance().getWriterQueueSize();

    // TODO: Comeup with a more sensible number.
    public static final BlockingQueue<Event> metricQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);

    private static final int NUM_RETRIES_FOR_TMP_FILE = 10;

    private static volatile boolean isMetricsLogEnabled = false;
    private static final int sTimeInterval =
            MetricsConfiguration.CONFIG_MAP.get(PerformanceAnalyzerMetrics.class).rotationInterval;

    /** This method aligns the given time with the ROTATION_INTERVAL */
    public static long getTimeInterval(long startTime) {
        return getTimeInterval(startTime, sTimeInterval);
    }

    public static long getTimeInterval(long startTime, int timeInterval) {
        return (startTime / timeInterval) * timeInterval;
    }

    public static String getCurrentTimeMetric() {
        return METRIC_CURRENT_TIME + sKeyValueDelimitor + System.currentTimeMillis();
    }

    public static String generatePath(long startTime, String... keysPath) {
        Path sDevShmLocationPath =
                Paths.get(PluginSettings.instance().getMetricsLocation())
                        .resolve(
                                Paths.get(
                                        String.valueOf(
                                                PerformanceAnalyzerMetrics.getTimeInterval(
                                                        startTime)),
                                        keysPath));
        return sDevShmLocationPath.toString();
    }

    public static void setIsMetricsLogEnabled(boolean enabled) {
        isMetricsLogEnabled = enabled;
    }

    public static void addMetricEntry(StringBuilder value, String metricKey, String metricValue) {
        value.append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor)
                .append(metricKey)
                .append(PerformanceAnalyzerMetrics.sKeyValueDelimitor)
                .append(metricValue);
    }

    public static void addMetricEntry(StringBuilder value, String metricKey, long metricValue) {
        value.append(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor)
                .append(metricKey)
                .append(PerformanceAnalyzerMetrics.sKeyValueDelimitor)
                .append(metricValue);
    }

    private static void emitMetric(BlockingQueue<Event> q, Event entry) {
        if (!q.offer(entry)) {
            PerformanceAnalyzerApp.ERRORS_AND_EXCEPTIONS_AGGREGATOR.updateStat(
                    ExceptionsAndErrors.METRICS_WRITE_ERROR, "", 1);
            LOG.debug("Could not enter metric {}", entry);
        }
    }

    static void emitMetric(long epoch, String metricKey, String value) {
        emitMetric(metricQueue, new Event(metricKey, value, epoch));
        if (isMetricsLogEnabled) {
            LOG.info(metricKey + "\n" + value);
        }
    }

    public static String getMetric(long startTime, String... keysPath) {
        return getMetric(generatePath(startTime, keysPath));
    }

    public static String getMetric(String keyPath) {
        try {
            return new String(Files.readAllBytes(Paths.get(keyPath)));
        } catch (Exception ex) {
            // -todo logging
            //            ex.printStackTrace();
            return "";
        }
    }

    public static String extractMetricValue(String metricVal, String key) {
        int startIndex = metricVal.indexOf(key);

        if (startIndex != -1) {
            startIndex = metricVal.indexOf(sKeyValueDelimitor, startIndex);
            int endIndex = metricVal.indexOf(sMetricNewLineDelimitor, startIndex + 1);

            if (endIndex == -1) {
                endIndex = metricVal.length();
            }
            return metricVal.substring(startIndex + 1, endIndex);
        }
        return null;
    }

    public static void removeMetrics(String keyPath) {
        removeMetrics(new File(keyPath));
    }

    public static void removeMetrics(File keyPathFile) {
        if (keyPathFile.isDirectory()) {
            String[] children = keyPathFile.list();
            if (children != null) {
                for (String child : children) {
                    removeMetrics(new File(keyPathFile, child));
                }
            }
        }
        try {
            if (!keyPathFile.delete()) {
                PerformanceAnalyzerApp.ERRORS_AND_EXCEPTIONS_AGGREGATOR.updateStat(
                        ExceptionsAndErrors.METRICS_REMOVE_ERROR, "", 1);
                LOG.debug("Purge Could not delete file {}", keyPathFile);
            }
        } catch (Exception ex) {
            PerformanceAnalyzerApp.ERRORS_AND_EXCEPTIONS_AGGREGATOR.updateStat(
                    ExceptionsAndErrors.METRICS_REMOVE_ERROR, "", 1);
            LOG.debug(
                    (Supplier<?>)
                            () ->
                                    new ParameterizedMessage(
                                            "Error in deleting file: {} for keyPath:{} with ExceptionCode: {}",
                                            ex.toString(),
                                            keyPathFile.getAbsolutePath(),
                                            ExceptionsAndErrors.METRICS_REMOVE_ERROR.toString()),
                    ex);
        }
    }

    public static String getJsonCurrentMilliSeconds() {
        return new StringBuilder()
                .append("{\"")
                .append(PerformanceAnalyzerMetrics.METRIC_CURRENT_TIME)
                .append("\"")
                .append(PerformanceAnalyzerMetrics.sKeyValueDelimitor)
                .append(System.currentTimeMillis())
                .append("}")
                .toString();
    }
}
