/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.tests.jvm.old_gen_policy.multi_node;

import static org.opensearch.performanceanalyzer.rca.integTests.tests.jvm.old_gen_policy.multi_node.LevelTwoMultiNodeITest.FIELDDATA_CACHE_SIZE_IN_PERCENT;
import static org.opensearch.performanceanalyzer.rca.integTests.tests.jvm.old_gen_policy.multi_node.LevelTwoMultiNodeITest.HEAP_MAX_SIZE_IN_BYTE;
import static org.opensearch.performanceanalyzer.rca.integTests.tests.jvm.old_gen_policy.multi_node.LevelTwoMultiNodeITest.SHARD_REQUEST_CACHE_SIZE_IN_PERCENT;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.opensearch.performanceanalyzer.decisionmaker.actions.configs.QueueActionConfig;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Cache_Max_Size;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.GC_Collection_Event;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Heap_Max;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Heap_Used;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.ThreadPool_QueueCapacity;
import org.opensearch.performanceanalyzer.rca.integTests.framework.RcaItMarker;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AClusterType;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AErrorPatternIgnored;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AExpect;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AMetric;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ARcaGraph;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ATable;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ATuple;
import org.opensearch.performanceanalyzer.rca.integTests.framework.configs.ClusterType;
import org.opensearch.performanceanalyzer.rca.integTests.framework.configs.HostTag;
import org.opensearch.performanceanalyzer.rca.integTests.framework.runners.RcaItNotEncryptedRunner;
import org.opensearch.performanceanalyzer.rca.integTests.tests.jvm.old_gen_policy.validator.LevelTwoValidator;
import org.opensearch.performanceanalyzer.rca.persistence.actions.PersistedAction;
import org.opensearch.performanceanalyzer.rca.store.OpenSearchAnalysisGraph;
import org.opensearch.performanceanalyzer.rca.store.rca.cache.CacheUtil;

@RunWith(RcaItNotEncryptedRunner.class)
@Category(RcaItMarker.class)
@AClusterType(ClusterType.MULTI_NODE_CO_LOCATED_CLUSTER_MANAGER)
@ARcaGraph(OpenSearchAnalysisGraph.class)
@AMetric(
        name = Heap_Used.class,
        dimensionNames = {AllMetrics.HeapDimension.Constants.TYPE_VALUE},
        tables = {
            @ATable(
                    hostTag = {HostTag.DATA_0},
                    tuple = {
                        @ATuple(
                                dimensionValues = AllMetrics.GCType.Constants.OLD_GEN_VALUE,
                                sum = HEAP_MAX_SIZE_IN_BYTE * 0.86,
                                avg = HEAP_MAX_SIZE_IN_BYTE * 0.86,
                                min = HEAP_MAX_SIZE_IN_BYTE * 0.86,
                                max = HEAP_MAX_SIZE_IN_BYTE * 0.86),
                    })
        })
@AMetric(
        name = Heap_Max.class,
        dimensionNames = {AllMetrics.HeapDimension.Constants.TYPE_VALUE},
        tables = {
            @ATable(
                    hostTag = {HostTag.DATA_0},
                    tuple = {
                        @ATuple(
                                dimensionValues = AllMetrics.GCType.Constants.HEAP_VALUE,
                                sum = HEAP_MAX_SIZE_IN_BYTE,
                                avg = HEAP_MAX_SIZE_IN_BYTE,
                                min = HEAP_MAX_SIZE_IN_BYTE,
                                max = HEAP_MAX_SIZE_IN_BYTE),
                    })
        })
@AMetric(
        name = GC_Collection_Event.class,
        dimensionNames = {AllMetrics.HeapDimension.Constants.TYPE_VALUE},
        tables = {
            @ATable(
                    hostTag = {HostTag.DATA_0},
                    tuple = {
                        @ATuple(
                                dimensionValues = AllMetrics.GCType.Constants.TOT_FULL_GC_VALUE,
                                sum = 1,
                                avg = 1,
                                min = 1,
                                max = 1),
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
                                sum = HEAP_MAX_SIZE_IN_BYTE * FIELDDATA_CACHE_SIZE_IN_PERCENT,
                                avg = HEAP_MAX_SIZE_IN_BYTE * FIELDDATA_CACHE_SIZE_IN_PERCENT,
                                min = HEAP_MAX_SIZE_IN_BYTE * FIELDDATA_CACHE_SIZE_IN_PERCENT,
                                max = HEAP_MAX_SIZE_IN_BYTE * FIELDDATA_CACHE_SIZE_IN_PERCENT),
                        @ATuple(
                                dimensionValues = {
                                    AllMetrics.CacheType.Constants.SHARD_REQUEST_CACHE_NAME
                                },
                                sum = HEAP_MAX_SIZE_IN_BYTE * SHARD_REQUEST_CACHE_SIZE_IN_PERCENT,
                                avg = HEAP_MAX_SIZE_IN_BYTE * SHARD_REQUEST_CACHE_SIZE_IN_PERCENT,
                                min = HEAP_MAX_SIZE_IN_BYTE * SHARD_REQUEST_CACHE_SIZE_IN_PERCENT,
                                max = HEAP_MAX_SIZE_IN_BYTE * SHARD_REQUEST_CACHE_SIZE_IN_PERCENT)
                    }),
        })
@AMetric(
        name = ThreadPool_QueueCapacity.class,
        dimensionNames = {AllMetrics.ThreadPoolDimension.Constants.TYPE_VALUE},
        tables = {
            @ATable(
                    hostTag = HostTag.DATA_0,
                    tuple = {
                        @ATuple(
                                dimensionValues = {AllMetrics.ThreadPoolType.Constants.WRITE_NAME},
                                sum = QueueActionConfig.DEFAULT_WRITE_QUEUE_UPPER_BOUND - 10,
                                avg = QueueActionConfig.DEFAULT_WRITE_QUEUE_UPPER_BOUND - 10,
                                min = QueueActionConfig.DEFAULT_WRITE_QUEUE_UPPER_BOUND - 10,
                                max = QueueActionConfig.DEFAULT_WRITE_QUEUE_UPPER_BOUND - 10),
                        @ATuple(
                                dimensionValues = {AllMetrics.ThreadPoolType.Constants.SEARCH_NAME},
                                sum = QueueActionConfig.DEFAULT_SEARCH_QUEUE_UPPER_BOUND - 10,
                                avg = QueueActionConfig.DEFAULT_SEARCH_QUEUE_UPPER_BOUND - 10,
                                min = QueueActionConfig.DEFAULT_SEARCH_QUEUE_UPPER_BOUND - 10,
                                max = QueueActionConfig.DEFAULT_SEARCH_QUEUE_UPPER_BOUND - 10)
                    })
        })
public class LevelTwoMultiNodeITest {
    public static final long HEAP_MAX_SIZE_IN_BYTE = 10 * CacheUtil.GB_TO_BYTES;
    public static final double FIELDDATA_CACHE_SIZE_IN_PERCENT = 0.3;
    public static final double SHARD_REQUEST_CACHE_SIZE_IN_PERCENT = 0.04;

    @Test
    @AExpect(
            what = AExpect.Type.REST_API,
            on = HostTag.ELECTED_CLUSTER_MANAGER,
            validator = LevelTwoValidator.class,
            forRca = PersistedAction.class,
            timeoutSeconds = 1000)
    @AErrorPatternIgnored(
            pattern = "CacheUtil:getCacheMaxSize()",
            reason = "Cache related configs are expected to be missing in this integ test")
    @AErrorPatternIgnored(
            pattern = "AggregateMetric:gather()",
            reason = "Cache metrics are expected to be missing in this integ test")
    @AErrorPatternIgnored(
            pattern = "SubscribeResponseHandler:onError()",
            reason =
                    "A unit test expressly calls SubscribeResponseHandler#onError, which writes an error log")
    @AErrorPatternIgnored(
            pattern = "SQLParsingUtil:readDataFromSqlResult()",
            reason = "Old gen metrics is expected to be missing in this integ test.")
    @AErrorPatternIgnored(
            pattern = "HighHeapUsageOldGenRca:operate()",
            reason = "Old gen rca is expected to be missing in this integ test.")
    @AErrorPatternIgnored(
            pattern = "ModifyCacheMaxSizeAction:build()",
            reason = "Node config cache is expected to be missing during shutdown")
    @AErrorPatternIgnored(
            pattern = "NodeConfigCollector:collectAndPublishMetric()",
            reason = "Shard request cache metrics is expected to be missing")
    @AErrorPatternIgnored(
            pattern = "CacheUtil:getCacheMaxSize()",
            reason = "Shard request cache metrics is expected to be missing.")
    @AErrorPatternIgnored(
            pattern = "HighHeapUsageYoungGenRca:operate()",
            reason = "YoungGen metrics is expected to be missing.")
    @AErrorPatternIgnored(
            pattern = "OldGenRca:getMaxHeapSizeOrDefault()",
            reason = "YoungGen metrics is expected to be missing.")
    @AErrorPatternIgnored(
            pattern = "OldGenRca:getMaxOldGenSizeOrDefault()",
            reason = "YoungGen metrics is expected to be missing.")
    public void testActionBuilder() {}
}
