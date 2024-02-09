/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.opensearch.performanceanalyzer.commons.event_process.Event;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.AdmissionControlDimension;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.AdmissionControlValue;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.CacheConfigDimension;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.CacheConfigValue;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.CircuitBreakerDimension;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.CircuitBreakerValue;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.ClusterManagerPendingTaskDimension;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.ClusterManagerPendingValue;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.DevicePartitionDimension;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.DevicePartitionValue;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.DiskDimension;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.DiskValue;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.ElectionTermValue;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.HeapDimension;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.HeapValue;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.IPDimension;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.IPValue;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.MetricName;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.ShardStatsDerivedDimension;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.ShardStatsValue;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.TCPDimension;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.TCPValue;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.ThreadPoolDimension;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.ThreadPoolValue;
import org.opensearch.performanceanalyzer.commons.metrics.PerformanceAnalyzerMetrics;

public final class MetricPropertiesConfig {

    /**
     * Find files under /dev/shm/performanceanalyzer/TS_BUCKET/metricPathElements
     *
     * @param metricPathElements path element array
     * @return a list of Files
     */
    static FileHandler createFileHandler(String... metricPathElements) {
        return new FileHandler() {
            @Override
            public List<File> findFiles4Metric(long startTimeThirtySecondBucket) {
                List<File> ret = new ArrayList<File>(1);
                StringBuilder sb = new StringBuilder();
                sb.append(getRootLocation());
                sb.append(startTimeThirtySecondBucket);

                for (String element : metricPathElements) {
                    sb.append(File.separator);
                    sb.append(element);
                }
                File metricFile = new File(sb.toString());
                if (metricFile.exists()) {
                    ret.add(metricFile);
                }
                return ret;
            }

            public List<Event> getMetricData(Map<String, List<Event>> metricDataMap) {
                Objects.requireNonNull(metricDataMap);
                List<Event> entries = metricDataMap.get(metricPathElements[0]);
                return (entries == null ? Collections.emptyList() : entries);
            }
        };
    }

    public static class ShardStatFileHandler extends FileHandler {
        @Override
        public List<File> findFiles4Metric(long timeBucket) {
            File indicesFolder =
                    new File(
                            this.getRootLocation()
                                    + File.separator
                                    + timeBucket
                                    + File.separator
                                    + PerformanceAnalyzerMetrics.sIndicesPath);

            if (!indicesFolder.exists()) {
                return Collections.emptyList();
            }

            List<File> metricFiles = new ArrayList<>();

            File[] files = indicesFolder.listFiles();
            if (files != null) {
                for (File indexFolder : files) {
                    if (indexFolder != null) {
                        File[] shardIdFiles = indexFolder.listFiles();
                        if (shardIdFiles != null) {
                            for (File shardIdFile : shardIdFiles) {
                                metricFiles.add(shardIdFile);
                            }
                        }
                    }
                }
            }
            return metricFiles;
        }

        // An example shard data can be:
        // ^indices/nyc_taxis/29
        // {"current_time":1566413966497}
        // {"Indexing_ThrottleTime":0,"Cache_Query_Hit":0,"Cache_Query_Miss":0,"Cache_Query_Size":0,
        // "Cache_FieldData_Eviction":0,"Cache_FieldData_Size":0,"Cache_Request_Hit":0,
        // "Cache_Request_Miss":0,"Cache_Request_Eviction":0,"Cache_Request_Size":0,"Refresh_Event":2,
        // "Refresh_Time":0,"Flush_Event":0,"Flush_Time":0,"Merge_Event":0,"Merge_Time":0,
        // "Merge_CurrentEvent":0,"Indexing_Buffer":0,"Segments_Total":0,"Segments_Memory":0,
        // "Terms_Memory":0,"StoredFields_Memory":0,"TermVectors_Memory":0,"Norms_Memory":0,
        // "Points_Memory":0,"DocValues_Memory":0,"IndexWriter_Memory":0,"VersionMap_Memory":0,"Bitset_Memory":0}$
        public List<Event> getMetricData(Map<String, List<Event>> metricDataMap) {
            Objects.requireNonNull(metricDataMap);
            return metricDataMap.computeIfAbsent(
                    PerformanceAnalyzerMetrics.sIndicesPath, k -> Collections.emptyList());
        }

        @Override
        public String filePathRegex() {
            // getRootLocation() may or may not end with File.separator.  So
            // I put ? next to File.separator.
            return getRootLocation()
                    + File.separator
                    + "?\\d+"
                    + File.separator
                    + PerformanceAnalyzerMetrics.sIndicesPath
                    + File.separator
                    + "(.*)"
                    + File.separator
                    + "(\\d+)";
        }
    }

    private final Map<MetricName, MetricProperties> metricName2Property;

    private static final MetricPropertiesConfig INSTANCE = new MetricPropertiesConfig();

    private Map<AllMetrics.MetricName, String> metricPathMap;
    private Map<String, AllMetrics.MetricName> eventKeyToMetricNameMap;

    private MetricPropertiesConfig() {
        metricPathMap = new HashMap<>();
        metricPathMap.put(MetricName.CACHE_CONFIG, PerformanceAnalyzerMetrics.sCacheConfigPath);
        metricPathMap.put(
                MetricName.CIRCUIT_BREAKER, PerformanceAnalyzerMetrics.sCircuitBreakerPath);
        metricPathMap.put(MetricName.HEAP_METRICS, PerformanceAnalyzerMetrics.sHeapPath);
        metricPathMap.put(MetricName.DISK_METRICS, PerformanceAnalyzerMetrics.sDisksPath);
        metricPathMap.put(MetricName.TCP_METRICS, PerformanceAnalyzerMetrics.sTCPPath);
        metricPathMap.put(MetricName.IP_METRICS, PerformanceAnalyzerMetrics.sIPPath);
        metricPathMap.put(MetricName.THREAD_POOL, PerformanceAnalyzerMetrics.sThreadPoolPath);
        metricPathMap.put(MetricName.SHARD_STATS, PerformanceAnalyzerMetrics.sIndicesPath);
        metricPathMap.put(
                MetricName.CLUSTER_MANAGER_PENDING, PerformanceAnalyzerMetrics.sPendingTasksPath);
        metricPathMap.put(
                MetricName.MOUNTED_PARTITION_METRICS,
                PerformanceAnalyzerMetrics.sMountedPartitionMetricsPath);
        metricPathMap.put(
                MetricName.CLUSTER_APPLIER_SERVICE,
                PerformanceAnalyzerMetrics.sClusterApplierService);
        metricPathMap.put(MetricName.ELECTION_TERM, PerformanceAnalyzerMetrics.sElectionTermPath);
        metricPathMap.put(
                MetricName.ADMISSION_CONTROL_METRICS,
                PerformanceAnalyzerMetrics.sAdmissionControlMetricsPath);
        metricPathMap.put(
                MetricName.SHARD_INDEXING_PRESSURE,
                PerformanceAnalyzerMetrics.sShardIndexingPressurePath);
        metricPathMap.put(
                MetricName.CLUSTER_MANAGER_CLUSTER_UPDATE_STATS,
                PerformanceAnalyzerMetrics.sClusterManagerClusterUpdate);

        eventKeyToMetricNameMap = new HashMap<>();
        eventKeyToMetricNameMap.put(
                PerformanceAnalyzerMetrics.sCacheConfigPath, MetricName.CACHE_CONFIG);
        eventKeyToMetricNameMap.put(
                PerformanceAnalyzerMetrics.sCircuitBreakerPath, MetricName.CIRCUIT_BREAKER);
        eventKeyToMetricNameMap.put(PerformanceAnalyzerMetrics.sHeapPath, MetricName.HEAP_METRICS);
        eventKeyToMetricNameMap.put(PerformanceAnalyzerMetrics.sDisksPath, MetricName.DISK_METRICS);
        eventKeyToMetricNameMap.put(PerformanceAnalyzerMetrics.sTCPPath, MetricName.TCP_METRICS);
        eventKeyToMetricNameMap.put(PerformanceAnalyzerMetrics.sIPPath, MetricName.IP_METRICS);
        eventKeyToMetricNameMap.put(
                PerformanceAnalyzerMetrics.sThreadPoolPath, MetricName.THREAD_POOL);
        eventKeyToMetricNameMap.put(
                PerformanceAnalyzerMetrics.sIndicesPath, MetricName.SHARD_STATS);
        eventKeyToMetricNameMap.put(
                PerformanceAnalyzerMetrics.sPendingTasksPath, MetricName.CLUSTER_MANAGER_PENDING);
        eventKeyToMetricNameMap.put(
                PerformanceAnalyzerMetrics.sMountedPartitionMetricsPath,
                MetricName.MOUNTED_PARTITION_METRICS);
        eventKeyToMetricNameMap.put(
                PerformanceAnalyzerMetrics.sClusterManagerClusterUpdate,
                MetricName.CLUSTER_MANAGER_CLUSTER_UPDATE_STATS);
        eventKeyToMetricNameMap.put(
                PerformanceAnalyzerMetrics.sClusterApplierService,
                MetricName.CLUSTER_APPLIER_SERVICE);
        eventKeyToMetricNameMap.put(
                PerformanceAnalyzerMetrics.sElectionTermPath, MetricName.ELECTION_TERM);
        eventKeyToMetricNameMap.put(
                PerformanceAnalyzerMetrics.sAdmissionControlMetricsPath,
                MetricName.ADMISSION_CONTROL_METRICS);
        eventKeyToMetricNameMap.put(
                PerformanceAnalyzerMetrics.sShardIndexingPressurePath,
                MetricName.SHARD_INDEXING_PRESSURE);

        metricName2Property = new HashMap<>();

        metricName2Property.put(
                MetricName.CACHE_CONFIG,
                new MetricProperties(
                        CacheConfigDimension.values(),
                        CacheConfigValue.values(),
                        createFileHandler(metricPathMap.get(MetricName.CACHE_CONFIG))));
        metricName2Property.put(
                MetricName.CIRCUIT_BREAKER,
                new MetricProperties(
                        CircuitBreakerDimension.values(),
                        CircuitBreakerValue.values(),
                        createFileHandler(metricPathMap.get(MetricName.CIRCUIT_BREAKER))));
        metricName2Property.put(
                MetricName.HEAP_METRICS,
                new MetricProperties(
                        HeapDimension.values(),
                        HeapValue.values(),
                        createFileHandler(metricPathMap.get(MetricName.HEAP_METRICS))));
        metricName2Property.put(
                MetricName.DISK_METRICS,
                new MetricProperties(
                        DiskDimension.values(),
                        DiskValue.values(),
                        createFileHandler(metricPathMap.get(MetricName.DISK_METRICS))));
        metricName2Property.put(
                MetricName.TCP_METRICS,
                new MetricProperties(
                        TCPDimension.values(),
                        TCPValue.values(),
                        createFileHandler(metricPathMap.get(MetricName.TCP_METRICS))));
        metricName2Property.put(
                MetricName.IP_METRICS,
                new MetricProperties(
                        IPDimension.values(),
                        IPValue.values(),
                        createFileHandler(metricPathMap.get(MetricName.IP_METRICS))));
        metricName2Property.put(
                MetricName.THREAD_POOL,
                new MetricProperties(
                        ThreadPoolDimension.values(),
                        ThreadPoolValue.values(),
                        createFileHandler(metricPathMap.get(MetricName.THREAD_POOL))));
        metricName2Property.put(
                MetricName.SHARD_STATS,
                new MetricProperties(
                        ShardStatsDerivedDimension.values(),
                        MetricProperties.EMPTY_DIMENSION,
                        ShardStatsValue.values(),
                        new ShardStatFileHandler()));
        metricName2Property.put(
                MetricName.CLUSTER_MANAGER_PENDING,
                new MetricProperties(
                        ClusterManagerPendingTaskDimension.values(),
                        ClusterManagerPendingValue.values(),
                        createFileHandler(
                                metricPathMap.get(MetricName.CLUSTER_MANAGER_PENDING),
                                PerformanceAnalyzerMetrics.CLUSTER_MANAGER_CURRENT,
                                PerformanceAnalyzerMetrics.CLUSTER_MANAGER_META_DATA)));
        metricName2Property.put(
                MetricName.MOUNTED_PARTITION_METRICS,
                new MetricProperties(
                        DevicePartitionDimension.values(),
                        DevicePartitionValue.values(),
                        createFileHandler(
                                metricPathMap.get(MetricName.MOUNTED_PARTITION_METRICS))));
        metricName2Property.put(
                MetricName.CLUSTER_APPLIER_SERVICE,
                new MetricProperties(
                        MetricProperties.EMPTY_DIMENSION,
                        AllMetrics.ClusterApplierServiceStatsValue.values(),
                        createFileHandler(metricPathMap.get(MetricName.CLUSTER_APPLIER_SERVICE))));
        metricName2Property.put(
                MetricName.ELECTION_TERM,
                new MetricProperties(
                        MetricProperties.EMPTY_DIMENSION,
                        ElectionTermValue.values(),
                        createFileHandler(metricPathMap.get(MetricName.ELECTION_TERM))));
        metricName2Property.put(
                MetricName.ADMISSION_CONTROL_METRICS,
                new MetricProperties(
                        AdmissionControlDimension.values(),
                        AdmissionControlValue.values(),
                        createFileHandler(
                                metricPathMap.get(MetricName.ADMISSION_CONTROL_METRICS))));
        metricName2Property.put(
                MetricName.SHARD_INDEXING_PRESSURE,
                new MetricProperties(
                        AllMetrics.ShardIndexingPressureDimension.values(),
                        AllMetrics.ShardIndexingPressureValue.values(),
                        createFileHandler(metricPathMap.get(MetricName.SHARD_INDEXING_PRESSURE))));
        metricName2Property.put(
                MetricName.CLUSTER_MANAGER_CLUSTER_UPDATE_STATS,
                new MetricProperties(
                        MetricProperties.EMPTY_DIMENSION,
                        AllMetrics.ClusterManagerClusterUpdateStatsValue.values(),
                        createFileHandler(
                                metricPathMap.get(
                                        MetricName.CLUSTER_MANAGER_CLUSTER_UPDATE_STATS))));
    }

    public static MetricPropertiesConfig getInstance() {
        return INSTANCE;
    }

    public MetricProperties getProperty(MetricName name) {
        return metricName2Property.get(name);
    }

    public Map<MetricName, String> getMetricPathMap() {
        return metricPathMap;
    }

    Map<String, MetricName> getEventKeyToMetricNameMap() {
        return eventKeyToMetricNameMap;
    }

    @VisibleForTesting
    Map<MetricName, MetricProperties> getMetricName2Property() {
        return metricName2Property;
    }
}
