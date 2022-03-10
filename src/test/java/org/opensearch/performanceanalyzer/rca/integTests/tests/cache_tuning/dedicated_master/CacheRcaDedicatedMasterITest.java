/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.tests.cache_tuning.dedicated_master;

import static org.opensearch.performanceanalyzer.rca.integTests.tests.cache_tuning.Constants.CACHE_TUNING_RESOURCES_DIR;
import static org.opensearch.performanceanalyzer.rca.integTests.tests.cache_tuning.Constants.INDEX_NAME;
import static org.opensearch.performanceanalyzer.rca.integTests.tests.cache_tuning.Constants.SHARD_ID;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Cache_FieldData_Eviction;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Cache_FieldData_Size;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Cache_Max_Size;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Cache_Request_Eviction;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Cache_Request_Hit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Cache_Request_Size;
import org.opensearch.performanceanalyzer.rca.integTests.framework.RcaItMarker;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AClusterType;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AErrorPatternIgnored;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AExpect;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AMetric;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ARcaConf;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ARcaGraph;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ATable;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ATuple;
import org.opensearch.performanceanalyzer.rca.integTests.framework.configs.ClusterType;
import org.opensearch.performanceanalyzer.rca.integTests.framework.configs.HostTag;
import org.opensearch.performanceanalyzer.rca.integTests.framework.runners.RcaItNotEncryptedRunner;
import org.opensearch.performanceanalyzer.rca.integTests.tests.cache_tuning.validator.FieldDataCacheValidator;
import org.opensearch.performanceanalyzer.rca.integTests.tests.cache_tuning.validator.ShardRequestCacheValidator;
import org.opensearch.performanceanalyzer.rca.store.OpenSearchAnalysisGraph;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.FieldDataCacheClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.ShardRequestCacheClusterRca;

@Category(RcaItMarker.class)
@RunWith(RcaItNotEncryptedRunner.class)
@AClusterType(ClusterType.MULTI_NODE_DEDICATED_MASTER)
@ARcaGraph(OpenSearchAnalysisGraph.class)
// specify a custom rca.conf to set the collector time periods to 5s to reduce runtime
@ARcaConf(dataNode = CACHE_TUNING_RESOURCES_DIR + "rca.conf")
@AMetric(
        name = Cache_FieldData_Size.class,
        dimensionNames = {
            AllMetrics.CommonDimension.Constants.INDEX_NAME_VALUE,
            AllMetrics.CommonDimension.Constants.SHARDID_VALUE
        },
        tables = {
            @ATable(
                    hostTag = HostTag.DATA_0,
                    tuple = {
                        @ATuple(
                                dimensionValues = {INDEX_NAME, SHARD_ID},
                                sum = 8500.0,
                                avg = 8500.0,
                                min = 8500.0,
                                max = 8500.0)
                    })
        })
@AMetric(
        name = Cache_FieldData_Eviction.class,
        dimensionNames = {
            AllMetrics.CommonDimension.Constants.INDEX_NAME_VALUE,
            AllMetrics.CommonDimension.Constants.SHARDID_VALUE
        },
        tables = {
            @ATable(
                    hostTag = HostTag.DATA_0,
                    tuple = {
                        @ATuple(
                                dimensionValues = {INDEX_NAME, SHARD_ID},
                                sum = 1.0,
                                avg = 1.0,
                                min = 1.0,
                                max = 1.0)
                    })
        })
@AMetric(
        name = Cache_Request_Size.class,
        dimensionNames = {
            AllMetrics.CommonDimension.Constants.INDEX_NAME_VALUE,
            AllMetrics.CommonDimension.Constants.SHARDID_VALUE
        },
        tables = {
            @ATable(
                    hostTag = HostTag.DATA_0,
                    tuple = {
                        @ATuple(
                                dimensionValues = {INDEX_NAME, SHARD_ID},
                                sum = 100.0,
                                avg = 100.0,
                                min = 100.0,
                                max = 100.0)
                    })
        })
@AMetric(
        name = Cache_Request_Eviction.class,
        dimensionNames = {
            AllMetrics.CommonDimension.Constants.INDEX_NAME_VALUE,
            AllMetrics.CommonDimension.Constants.SHARDID_VALUE
        },
        tables = {
            @ATable(
                    hostTag = HostTag.DATA_0,
                    tuple = {
                        @ATuple(
                                dimensionValues = {INDEX_NAME, SHARD_ID},
                                sum = 1.0,
                                avg = 1.0,
                                min = 1.0,
                                max = 1.0)
                    })
        })
@AMetric(
        name = Cache_Request_Hit.class,
        dimensionNames = {
            AllMetrics.CommonDimension.Constants.INDEX_NAME_VALUE,
            AllMetrics.CommonDimension.Constants.SHARDID_VALUE
        },
        tables = {
            @ATable(
                    hostTag = HostTag.DATA_0,
                    tuple = {
                        @ATuple(
                                dimensionValues = {INDEX_NAME, SHARD_ID},
                                sum = 1.0,
                                avg = 1.0,
                                min = 1.0,
                                max = 1.0)
                    })
        })
@AMetric(
        name = Cache_Max_Size.class,
        dimensionNames = {AllMetrics.CacheConfigDimension.Constants.TYPE_VALUE},
        tables = {
            @ATable(
                    hostTag = HostTag.DATA_0,
                    tuple = {
                        @ATuple(
                                dimensionValues = {
                                    AllMetrics.CacheType.Constants.FIELD_DATA_CACHE_NAME
                                },
                                sum = 10000.0,
                                avg = 10000.0,
                                min = 10000.0,
                                max = 10000.0),
                        @ATuple(
                                dimensionValues = {
                                    AllMetrics.CacheType.Constants.SHARD_REQUEST_CACHE_NAME
                                },
                                sum = 100.0,
                                avg = 100.0,
                                min = 100.0,
                                max = 100.0)
                    }),
            @ATable(
                    hostTag = HostTag.ELECTED_MASTER,
                    tuple = {
                        @ATuple(
                                dimensionValues = {
                                    AllMetrics.CacheType.Constants.FIELD_DATA_CACHE_NAME
                                },
                                sum = 10000.0,
                                avg = 10000.0,
                                min = 10000.0,
                                max = 10000.0),
                        @ATuple(
                                dimensionValues = {
                                    AllMetrics.CacheType.Constants.SHARD_REQUEST_CACHE_NAME
                                },
                                sum = 100.0,
                                avg = 100.0,
                                min = 100.0,
                                max = 100.0)
                    })
        })
public class CacheRcaDedicatedMasterITest {
    // Test FieldDataCacheClusterRca.
    // This rca should be un-healthy when cache size is higher than threshold with evictions.
    @Test
    @AExpect(
            what = AExpect.Type.REST_API,
            on = HostTag.ELECTED_MASTER,
            validator = FieldDataCacheValidator.class,
            forRca = FieldDataCacheClusterRca.class,
            timeoutSeconds = 700)
    @AErrorPatternIgnored(
            pattern = "AggregateMetric:gather()",
            reason = "CPU metrics are expected to be missing in this integ test")
    @AErrorPatternIgnored(
            pattern = "Metric:gather()",
            reason = "Metrics are expected to be missing in this integ test")
    @AErrorPatternIgnored(
            pattern = "NodeConfigCacheReaderUtil",
            reason = "Node Config Cache are expected to be missing in this integ test.")
    @AErrorPatternIgnored(
            pattern = "CacheUtil:getCacheMaxSize()",
            reason = "Cache Metrics is expected to be missing in dedicated master.")
    @AErrorPatternIgnored(
            pattern = "ModifyCacheMaxSizeAction:build()",
            reason = "Heap metrics is expected to be missing in this integ test.")
    @AErrorPatternIgnored(
            pattern = "SubscribeResponseHandler:onError()",
            reason =
                    "A unit test expressly calls SubscribeResponseHandler#onError, which writes an error log")
    public void testFieldDataCacheRca() {}

    // Test ShardRequestCacheClusterRca.
    // This rca should be un-healthy when cache size is higher than threshold with evictions and
    // hits.
    @Test
    @AExpect(
            what = AExpect.Type.REST_API,
            on = HostTag.ELECTED_MASTER,
            validator = ShardRequestCacheValidator.class,
            forRca = ShardRequestCacheClusterRca.class,
            timeoutSeconds = 700)
    @AErrorPatternIgnored(
            pattern = "AggregateMetric:gather()",
            reason = "CPU metrics are expected to be missing in this integ test")
    @AErrorPatternIgnored(
            pattern = "Metric:gather()",
            reason = "Metrics are expected to be missing in this integ test")
    @AErrorPatternIgnored(
            pattern = "NodeConfigCacheReaderUtil",
            reason = "Node config cache metrics are expected to be missing in this integ test.")
    @AErrorPatternIgnored(
            pattern = "CacheUtil:getCacheMaxSize()",
            reason = "Cache Metrics is expected to be missing in dedicated master.")
    @AErrorPatternIgnored(
            pattern = "ModifyCacheMaxSizeAction:build()",
            reason = "Heap metrics is expected to be missing in this integ test.")
    @AErrorPatternIgnored(
            pattern = "SubscribeResponseHandler:onError()",
            reason =
                    "A unit test expressly calls SubscribeResponseHandler#onError, which writes an error log")
    public void testShardRequestCacheRca() {}
}
