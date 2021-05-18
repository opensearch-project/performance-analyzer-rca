/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

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
