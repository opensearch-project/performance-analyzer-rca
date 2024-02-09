/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.grpc.MetricEnum;
import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.store.collector.NodeConfigCache;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

public class NodeConfigCacheReaderUtil {
    private static final Logger LOG = LogManager.getLogger(NodeConfigCacheReaderUtil.class);

    public static Integer readQueueCapacity(
            final NodeConfigCache nodeConfigCache,
            final NodeKey nodeKey,
            final ResourceEnum resourceEnum) {
        final Resource resource =
                Resource.newBuilder()
                        .setResourceEnum(resourceEnum)
                        .setMetricEnum(MetricEnum.QUEUE_CAPACITY)
                        .build();
        try {
            return (int) nodeConfigCache.get(nodeKey, resource);
        } catch (final IllegalArgumentException e) {
            LOG.error("Exception while reading queue capacity from Node Config Cache", e);
        }
        return null;
    }

    public static Long readCacheMaxSizeInBytes(
            final NodeConfigCache nodeConfigCache,
            final NodeKey nodeKey,
            final ResourceEnum cacheType) {
        try {
            if (cacheType.equals(ResourceEnum.FIELD_DATA_CACHE)) {
                return (long) nodeConfigCache.get(nodeKey, ResourceUtil.FIELD_DATA_CACHE_MAX_SIZE);
            }
            return (long) nodeConfigCache.get(nodeKey, ResourceUtil.SHARD_REQUEST_CACHE_MAX_SIZE);
        } catch (final IllegalArgumentException e) {
            LOG.error("Exception while reading cache max size from Node Config Cache", e);
        }
        return null;
    }

    public static Long readHeapMaxSizeInBytes(
            final NodeConfigCache nodeConfigCache, final NodeKey nodeKey) {
        try {
            return (long) nodeConfigCache.get(nodeKey, ResourceUtil.HEAP_MAX_SIZE);
        } catch (final IllegalArgumentException e) {
            LOG.error("Exception while reading heap max size from Node Config Cache", e);
        }
        return null;
    }
}
