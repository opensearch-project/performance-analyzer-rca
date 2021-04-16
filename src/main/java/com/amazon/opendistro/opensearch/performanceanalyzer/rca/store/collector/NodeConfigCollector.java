/*
 * Copyright 2020-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazon.opendistro.opensearch.performanceanalyzer.rca.store.collector;


import com.amazon.opendistro.opensearch.performanceanalyzer.grpc.Resource;
import com.amazon.opendistro.opensearch.performanceanalyzer.metrics.AllMetrics;
import com.amazon.opendistro.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.framework.api.OpenSearchConfigNode;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.framework.api.flow_units.NodeConfigFlowUnit;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.framework.api.metrics.Cache_Max_Size;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.framework.api.metrics.Heap_Max;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.framework.api.metrics.ThreadPool_QueueCapacity;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.framework.api.persist.SQLParsingUtil;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
     * the config settings across grpc and send them to Decision Maker on elected master.
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
