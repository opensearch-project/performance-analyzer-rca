/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.collector;

import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.framework.api.OpenSearchConfigNode;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.NodeConfigFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Cache_Max_Size;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Heap_Max;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.ThreadPool_QueueCapacity;
import org.opensearch.performanceanalyzer.rca.framework.api.persist.SQLParsingUtil;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

/**
 * This is a node level collector in RCA graph which collect the current config settings from
 * OpenSearch (queue/cache capacity etc.) And pass them down to Decision Maker for the next round of
 * resource auto-tuning.
 */
public class NodeConfigCollector extends OpenSearchConfigNode {

    private static final Logger LOG = LogManager.getLogger(NodeConfigCollector.class);
    private final ThreadPool_QueueCapacity threadPool_queueCapacity;
    private final Cache_Max_Size cacheMaxSize;
    private final Heap_Max heapMaxSize;
    private final int rcaPeriod;
    private int counter;
    private final HashMap<Resource, Double> configResult;

    public NodeConfigCollector(
            int rcaPeriod,
            ThreadPool_QueueCapacity threadPool_queueCapacity,
            Cache_Max_Size cacheMaxSize,
            Heap_Max heapMaxSize) {
        this.threadPool_queueCapacity = threadPool_queueCapacity;
        this.cacheMaxSize = cacheMaxSize;
        this.heapMaxSize = heapMaxSize;
        this.rcaPeriod = rcaPeriod;
        this.counter = 0;
        this.configResult = new HashMap<>();
    }

    private void collectQueueCapacity(MetricFlowUnit flowUnit) {
        final double writeQueueCapacity =
                SQLParsingUtil.readDataFromSqlResult(
                        flowUnit.getData(),
                        AllMetrics.ThreadPoolDimension.THREAD_POOL_TYPE.getField(),
                        AllMetrics.ThreadPoolType.WRITE.toString(),
                        MetricsDB.MAX);
        collectAndPublishMetric(ResourceUtil.WRITE_QUEUE_CAPACITY, writeQueueCapacity);

        final double searchQueueCapacity =
                SQLParsingUtil.readDataFromSqlResult(
                        flowUnit.getData(),
                        AllMetrics.ThreadPoolDimension.THREAD_POOL_TYPE.getField(),
                        AllMetrics.ThreadPoolType.SEARCH.toString(),
                        MetricsDB.MAX);
        collectAndPublishMetric(ResourceUtil.SEARCH_QUEUE_CAPACITY, searchQueueCapacity);
    }

    private void collectCacheMaxSize(MetricFlowUnit cacheMaxSize) {
        final double fieldDataCacheMaxSize =
                SQLParsingUtil.readDataFromSqlResult(
                        cacheMaxSize.getData(),
                        AllMetrics.CacheConfigDimension.CACHE_TYPE.getField(),
                        AllMetrics.CacheType.FIELD_DATA_CACHE.toString(),
                        MetricsDB.MAX);
        collectAndPublishMetric(ResourceUtil.FIELD_DATA_CACHE_MAX_SIZE, fieldDataCacheMaxSize);

        final double shardRequestCacheMaxSize =
                SQLParsingUtil.readDataFromSqlResult(
                        cacheMaxSize.getData(),
                        AllMetrics.CacheConfigDimension.CACHE_TYPE.getField(),
                        AllMetrics.CacheType.SHARD_REQUEST_CACHE.toString(),
                        MetricsDB.MAX);
        collectAndPublishMetric(
                ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE, shardRequestCacheMaxSize);
    }

    private void collectHeapStats(MetricFlowUnit heapMax) {
        // total maximum heap size
        final double heapMaxSize =
                SQLParsingUtil.readDataFromSqlResult(
                        heapMax.getData(),
                        AllMetrics.HeapDimension.MEM_TYPE.getField(),
                        AllMetrics.GCType.HEAP.toString(),
                        MetricsDB.MAX);
        collectAndPublishMetric(ResourceUtil.HEAP_MAX_SIZE, heapMaxSize);
        // maximum old generation heap size
        final double oldGenMaxSize =
                SQLParsingUtil.readDataFromSqlResult(
                        heapMax.getData(),
                        AllMetrics.HeapDimension.MEM_TYPE.getField(),
                        AllMetrics.GCType.OLD_GEN.toString(),
                        MetricsDB.MAX);
        collectAndPublishMetric(ResourceUtil.OLD_GEN_MAX_SIZE, oldGenMaxSize);
        // maximum young generation heap size
        final double edenMaxSize =
                SQLParsingUtil.readDataFromSqlResult(
                        heapMax.getData(),
                        AllMetrics.HeapDimension.MEM_TYPE.getField(),
                        AllMetrics.GCType.EDEN.toString(),
                        MetricsDB.MAX);
        final double survivorMaxSize =
                SQLParsingUtil.readDataFromSqlResult(
                        heapMax.getData(),
                        AllMetrics.HeapDimension.MEM_TYPE.getField(),
                        AllMetrics.GCType.SURVIVOR.toString(),
                        MetricsDB.MAX);
        collectAndPublishMetric(
                ResourceUtil.YOUNG_GEN_MAX_SIZE, edenMaxSize + (2 * survivorMaxSize));
    }

    private void collectAndPublishMetric(final Resource resource, final double metricValue) {
        if (!Double.isNaN(metricValue)) {
            final NodeConfigCache nodeConfigCache = getAppContext().getNodeConfigCache();
            final NodeKey nodeKey = new NodeKey(getAppContext().getMyInstanceDetails());
            configResult.put(resource, metricValue);
            nodeConfigCache.put(nodeKey, resource, metricValue);
        } else {
            LOG.error("Metric value is NaN for resource:" + resource.toString());
        }
    }

    /**
     * collect config settings from the upstream metric flowunits and set them into the protobuf
     * message PerformanceControllerConfiguration. This will allow us to serialize / de-serialize
     * the config settings across grpc and send them to Decision Maker on elected cluster_manager.
     *
     * @return ResourceFlowUnit with HotNodeSummary. And HotNodeSummary carries
     *     PerformanceControllerConfiguration
     */
    @Override
    public NodeConfigFlowUnit operate() {
        counter += 1;
        for (MetricFlowUnit flowUnit : threadPool_queueCapacity.getFlowUnits()) {
            if (flowUnit.isEmpty()) {
                continue;
            }
            collectQueueCapacity(flowUnit);
        }
        for (MetricFlowUnit flowUnit : cacheMaxSize.getFlowUnits()) {
            if (flowUnit.isEmpty()) {
                continue;
            }
            collectCacheMaxSize(flowUnit);
        }
        for (MetricFlowUnit flowUnit : heapMaxSize.getFlowUnits()) {
            if (flowUnit.isEmpty()) {
                continue;
            }
            collectHeapStats(flowUnit);
        }

        if (counter == rcaPeriod) {
            counter = 0;
            NodeConfigFlowUnit flowUnits =
                    new NodeConfigFlowUnit(
                            System.currentTimeMillis(), new NodeKey(getInstanceDetails()));
            configResult.forEach(flowUnits::addConfig);
            // Clear the hashmap to avoid sending stale data
            configResult.clear();
            return flowUnits;
        } else {
            return new NodeConfigFlowUnit(System.currentTimeMillis());
        }
    }
}
