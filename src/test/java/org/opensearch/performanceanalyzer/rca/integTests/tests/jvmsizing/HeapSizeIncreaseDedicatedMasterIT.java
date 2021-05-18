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

package org.opensearch.performanceanalyzer.rca.integTests.tests.jvmsizing;


import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.GC_Collection_Event;
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
import org.opensearch.performanceanalyzer.rca.integTests.tests.jvmsizing.validator.HeapSizeIncreaseValidatorDedicatedMaster;
import org.opensearch.performanceanalyzer.rca.persistence.actions.PersistedAction;
import org.opensearch.performanceanalyzer.rca.store.OpenSearchAnalysisGraph;

@Category(RcaItMarker.class)
@RunWith(RcaItNotEncryptedRunner.class)
@AClusterType(ClusterType.MULTI_NODE_DEDICATED_MASTER)
@ARcaGraph(OpenSearchAnalysisGraph.class)
@ARcaConf(
        dataNode = JvmSizingITConstants.RCA_CONF_PATH + "rca.conf",
        electedMaster = JvmSizingITConstants.RCA_CONF_PATH + "rca_master.conf")
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
                    hostTag = HostTag.DATA_1,
                    tuple = {
                        @ATuple(
                                dimensionValues = {AllMetrics.GCType.Constants.OLD_GEN_VALUE},
                                sum = 1000000000.0,
                                avg = 1000000000.0,
                                min = 1000000000.0,
                                max = 1000000000.0)
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
                    hostTag = HostTag.DATA_1,
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
                    hostTag = HostTag.DATA_1,
                    tuple = {
                        @ATuple(
                                dimensionValues = {AllMetrics.GCType.Constants.TOT_FULL_GC_VALUE},
                                sum = 10.0,
                                avg = 10.0,
                                max = 10.0,
                                min = 10.0)
                    })
        })
public class HeapSizeIncreaseDedicatedMasterIT {

    @Test
    @AExpect(
            what = AExpect.Type.DB_QUERY,
            on = HostTag.ELECTED_MASTER,
            validator = HeapSizeIncreaseValidatorDedicatedMaster.class,
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
            reason =
                    "This method is called by HighHeapUsageOldGenRca which is not part of the jvm "
                            + "sizing rcas.")
    @AErrorPatternIgnored(
            pattern = "BucketizedSlidingWindow:next()",
            reason =
                    "Since the persistence path can be null for integration test, calls to next() is "
                            + "expected to fail")
    public void testHeapSizeIncreaseDedicatedMaster() {}
}
