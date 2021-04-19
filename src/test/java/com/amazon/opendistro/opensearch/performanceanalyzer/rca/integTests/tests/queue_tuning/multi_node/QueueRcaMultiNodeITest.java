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

package com.amazon.opendistro.opensearch.performanceanalyzer.rca.integTests.tests.queue_tuning.multi_node;

import static com.amazon.opendistro.opensearch.performanceanalyzer.rca.integTests.tests.queue_tuning.Constants.QUEUE_TUNING_RESOURCES_DIR;

import com.amazon.opendistro.opensearch.performanceanalyzer.metrics.AllMetrics;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.framework.api.metrics.ThreadPool_QueueCapacity;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.framework.api.metrics.ThreadPool_RejectedReqs;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.integTests.framework.RcaItMarker;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AClusterType;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AErrorPatternIgnored;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AExpect;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AMetric;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ARcaConf;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ARcaGraph;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ATable;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ATuple;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.integTests.framework.configs.ClusterType;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.integTests.framework.configs.HostTag;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.integTests.framework.runners.RcaItNotEncryptedRunner;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.integTests.tests.queue_tuning.validator.QueueRejectionValidator;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.store.OpenSearchAnalysisGraph;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.store.rca.cluster.QueueRejectionClusterRca;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(RcaItNotEncryptedRunner.class)
@Category(RcaItMarker.class)
@AClusterType(ClusterType.MULTI_NODE_CO_LOCATED_MASTER)
@ARcaGraph(OpenSearchAnalysisGraph.class)
// specify a custom rca.conf to set the rejection-time-period-in-seconds to 5s to reduce runtime
@ARcaConf(dataNode = QUEUE_TUNING_RESOURCES_DIR + "rca.conf")
@AMetric(
        name = ThreadPool_RejectedReqs.class,
        dimensionNames = {AllMetrics.ThreadPoolDimension.Constants.TYPE_VALUE},
        tables = {
            @ATable(
                    hostTag = HostTag.DATA_0,
                    tuple = {
                        @ATuple(
                                dimensionValues = {AllMetrics.ThreadPoolType.Constants.WRITE_NAME},
                                sum = 1.0,
                                avg = 1.0,
                                min = 1.0,
                                max = 1.0),
                        @ATuple(
                                dimensionValues = {AllMetrics.ThreadPoolType.Constants.SEARCH_NAME},
                                sum = 0.0,
                                avg = 0.0,
                                min = 0.0,
                                max = 0.0)
                    })
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
                                sum = 500,
                                avg = 500,
                                min = 500,
                                max = 500),
                        @ATuple(
                                dimensionValues = {AllMetrics.ThreadPoolType.Constants.SEARCH_NAME},
                                sum = 1500,
                                avg = 1500,
                                min = 1500,
                                max = 1500)
                    })
        })
public class QueueRcaMultiNodeITest {
    // This integ test is built to test queue rejection RCA + queue rejection cluster RCA
    // This test injects queue rejection metrics on one of the data node and queries the
    // rest API on master to check whether queue rejection cluster RCA becomes unhealthy
    @Test
    @AExpect(
            what = AExpect.Type.REST_API,
            on = HostTag.ELECTED_MASTER,
            validator = QueueRejectionValidator.class,
            forRca = QueueRejectionClusterRca.class,
            timeoutSeconds = 500)
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
    public void testQueueRejectionRca() {}
}
