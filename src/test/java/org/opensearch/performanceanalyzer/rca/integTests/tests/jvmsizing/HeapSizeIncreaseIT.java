/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.tests.jvmsizing;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.GC_Collection_Event;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.GC_Type;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Heap_Max;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Heap_Used;
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
import org.opensearch.performanceanalyzer.rca.integTests.tests.jvmsizing.validator.HeapSizeIncreaseValidatorCollocatedClusterManager;
import org.opensearch.performanceanalyzer.rca.persistence.actions.PersistedAction;
import org.opensearch.performanceanalyzer.rca.store.OpenSearchAnalysisGraph;

@Category(RcaItMarker.class)
@RunWith(RcaItNotEncryptedRunner.class)
@AClusterType(ClusterType.MULTI_NODE_CO_LOCATED_CLUSTER_MANAGER)
@ARcaGraph(OpenSearchAnalysisGraph.class)
@ARcaConf(
        dataNode = JvmSizingITConstants.RCA_CONF_PATH + "rca.conf",
        electedClusterManager = JvmSizingITConstants.RCA_CONF_PATH + "rca_cluster_manager.conf")
@AMetric(
        name = Heap_Max.class,
        dimensionNames = {AllMetrics.HeapDimension.Constants.TYPE_VALUE},
        tables = {
            @ATable(
                    hostTag = HostTag.DATA_0,
                    tuple = {
                        @ATuple(
                                dimensionValues = {AllMetrics.GCType.Constants.OLD_GEN_VALUE},
                                sum = 1000000000.0,
                                avg = 1000000000.0,
                                min = 1000000000.0,
                                max = 1000000000.0)
                    }),
            @ATable(
                    hostTag = HostTag.ELECTED_CLUSTER_MANAGER,
                    tuple = {
                        @ATuple(
                                dimensionValues = {AllMetrics.GCType.Constants.OLD_GEN_VALUE},
                                sum = 1000000000.0,
                                avg = 1000000000.0,
                                max = 1000000000.0,
                                min = 1000000000.0)
                    })
        })
@AMetric(
        name = Heap_Used.class,
        dimensionNames = {AllMetrics.HeapDimension.Constants.TYPE_VALUE},
        tables = {
            @ATable(
                    hostTag = HostTag.DATA_0,
                    tuple = {
                        @ATuple(
                                dimensionValues = {AllMetrics.GCType.Constants.OLD_GEN_VALUE},
                                sum = 950000000.0,
                                avg = 950000000.0,
                                min = 950000000.0,
                                max = 950000000.0)
                    }),
            @ATable(
                    hostTag = HostTag.ELECTED_CLUSTER_MANAGER,
                    tuple = {
                        @ATuple(
                                dimensionValues = {AllMetrics.GCType.Constants.OLD_GEN_VALUE},
                                sum = 950000000.0,
                                avg = 950000000.0,
                                min = 950000000.0,
                                max = 950000000.0)
                    })
        })
@AMetric(
        name = GC_Collection_Event.class,
        dimensionNames = {AllMetrics.HeapDimension.Constants.TYPE_VALUE},
        tables = {
            @ATable(
                    hostTag = HostTag.DATA_0,
                    tuple = {
                        @ATuple(
                                dimensionValues = {AllMetrics.GCType.Constants.TOT_FULL_GC_VALUE},
                                sum = 10.0,
                                avg = 10.0,
                                max = 10.0,
                                min = 10.0)
                    }),
            @ATable(
                    hostTag = HostTag.ELECTED_CLUSTER_MANAGER,
                    tuple = {
                        @ATuple(
                                dimensionValues = {AllMetrics.GCType.Constants.TOT_FULL_GC_VALUE},
                                sum = 10.0,
                                avg = 10.0,
                                max = 10.0,
                                min = 10.0)
                    })
        })
@AMetric(
        name = GC_Type.class,
        dimensionNames = {
            AllMetrics.GCInfoDimension.Constants.MEMORY_POOL_VALUE,
            AllMetrics.GCInfoDimension.Constants.COLLECTOR_NAME_VALUE
        },
        tables = {
            @ATable(
                    hostTag = HostTag.DATA_0,
                    tuple = {
                        @ATuple(
                                dimensionValues = {
                                    AllMetrics.GCType.Constants.OLD_GEN_VALUE,
                                    JvmSizingITConstants.CMS_COLLECTOR
                                },
                                sum = 10.0,
                                avg = 10.0,
                                max = 10.0,
                                min = 10.0)
                    }),
            @ATable(
                    hostTag = HostTag.ELECTED_CLUSTER_MANAGER,
                    tuple = {
                        @ATuple(
                                dimensionValues = {
                                    AllMetrics.GCType.Constants.OLD_GEN_VALUE,
                                    JvmSizingITConstants.CMS_COLLECTOR
                                },
                                sum = 10.0,
                                avg = 10.0,
                                max = 10.0,
                                min = 10.0)
                    })
        })
public class HeapSizeIncreaseIT {

    @Test
    @AExpect(
            what = AExpect.Type.DB_QUERY,
            on = HostTag.ELECTED_CLUSTER_MANAGER,
            validator = HeapSizeIncreaseValidatorCollocatedClusterManager.class,
            forRca = PersistedAction.class,
            timeoutSeconds = 190)
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
            pattern = "PersistableSlidingWindow:<init>()",
            reason = "Persistence base path can be null for integration test.")
    @AErrorPatternIgnored(
            pattern = "OldGenRca:getMaxHeapSizeOrDefault()",
            reason = "YoungGen metrics is expected to be missing.")
    @AErrorPatternIgnored(
            pattern = "BucketizedSlidingWindow:next()",
            reason =
                    "Since the persistence path can be null for integration test, calls to next() is "
                            + "expected to fail")
    public void testHeapSizeIncrease() {}
}
