/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.tests.jvm.young_gen;

import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.GC_Collection_Event;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.GC_Collection_Time;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.GC_Type;
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
import org.opensearch.performanceanalyzer.rca.integTests.tests.jvm.young_gen.validator.YoungGenNonBreachingValidator;
import org.opensearch.performanceanalyzer.rca.integTests.tests.jvmsizing.JvmSizingITConstants;
import org.opensearch.performanceanalyzer.rca.persistence.actions.PersistedAction;
import org.opensearch.performanceanalyzer.rca.store.OpenSearchAnalysisGraph;

/**
 * Negative test: Tests that the action is NOT emitted if the threshold is not breached. The
 * threshold is controlled by the rca.conf and rca_cluster_manager.conf files.
 */
@Category(RcaItMarker.class)
@RunWith(RcaItNotEncryptedRunner.class)
@AClusterType(ClusterType.MULTI_NODE_CO_LOCATED_CLUSTER_MANAGER)
@ARcaGraph(OpenSearchAnalysisGraph.class)
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
        name = GC_Collection_Time.class,
        dimensionNames = {AllMetrics.HeapDimension.Constants.TYPE_VALUE},
        tables = {
            @ATable(
                    hostTag = HostTag.DATA_0,
                    tuple = {
                        @ATuple(
                                dimensionValues = {AllMetrics.GCType.Constants.TOT_FULL_GC_VALUE},
                                sum = 5000,
                                avg = 5000,
                                min = 5000,
                                max = 5000),
                        @ATuple(
                                dimensionValues = {AllMetrics.GCType.Constants.TOT_YOUNG_GC_VALUE},
                                sum = 500,
                                avg = 500,
                                min = 500,
                                max = 500)
                    }),
            @ATable(
                    hostTag = HostTag.ELECTED_CLUSTER_MANAGER,
                    tuple = {
                        @ATuple(
                                dimensionValues = {AllMetrics.GCType.Constants.TOT_FULL_GC_VALUE},
                                sum = 5000,
                                avg = 5000,
                                min = 5000,
                                max = 5000),
                        @ATuple(
                                dimensionValues = {AllMetrics.GCType.Constants.TOT_YOUNG_GC_VALUE},
                                sum = 500,
                                avg = 500,
                                min = 500,
                                max = 500)
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
public class YoungGenHighThresholdTest {

    public static final int SLEEP_DURATION_IN_S = 190;

    @Test
    @ARcaConf(
            dataNode = YoungGenITConstants.RCA_HIGH_THRESHOLD_CONF_PATH,
            electedClusterManager = YoungGenITConstants.RCA_CLUSTER_MANAGER_CONF_PATH)
    @AExpect(
            what = AExpect.Type.REST_API,
            on = HostTag.ELECTED_CLUSTER_MANAGER,
            validator = YoungGenNonBreachingValidator.class,
            forRca = PersistedAction.class,
            timeoutSeconds = 240)
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
    public void testDataNodeThresholdNotBreached() {
        // We know that it takes at most 180 seconds(in the case of a multinode cluster) to fire the
        // action based on the decider and rca thresholds set for integration tests.
        // In order to prove that no action has been taken, we sleep for 190 seconds in the test
        // while the rest of the framework goes on emitting metrics, ticking the scheduler etc. On
        // wakeup, we check if the actions table contains the relevant row.

        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(SLEEP_DURATION_IN_S));
        } catch (InterruptedException e) {
            throw new RuntimeException("Sleep was interrupted. Underlying exception: ", e);
        }
    }

    @Test
    @ARcaConf(
            dataNode = YoungGenITConstants.RCA_CONF_PATH,
            electedClusterManager =
                    YoungGenITConstants.RCA_CLUSTER_MANAGER_HIGH_THRESHOLD_CONF_PATH)
    @AExpect(
            what = AExpect.Type.REST_API,
            on = HostTag.ELECTED_CLUSTER_MANAGER,
            validator = YoungGenNonBreachingValidator.class,
            forRca = PersistedAction.class,
            timeoutSeconds = 240)
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
    public void testClusterManagerNodeThresholdNotBreached() {
        // Same reasoning as the test case above.
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(SLEEP_DURATION_IN_S));
        } catch (InterruptedException e) {
            throw new RuntimeException("Sleep was interrupted. Underlying exception: ", e);
        }
    }
}
