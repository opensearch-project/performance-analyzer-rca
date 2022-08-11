/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.tests.queue_tuning.multi_node;

import static org.opensearch.performanceanalyzer.rca.integTests.tests.queue_tuning.Constants.QUEUE_TUNING_RESOURCES_DIR;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.ThreadPool_QueueCapacity;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.ThreadPool_RejectedReqs;
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
import org.opensearch.performanceanalyzer.rca.integTests.tests.queue_tuning.validator.QueueDeciderValidator;
import org.opensearch.performanceanalyzer.rca.persistence.actions.PersistedAction;
import org.opensearch.performanceanalyzer.rca.store.OpenSearchAnalysisGraph;

@RunWith(RcaItNotEncryptedRunner.class)
@Category(RcaItMarker.class)
@AClusterType(ClusterType.MULTI_NODE_DEDICATED_CLUSTER_MANAGER)
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
public class QueueDeciderMultiNodeITest {
    // This integ test is built to test Decision Maker framework and queue remediation actions
    // This test injects queue rejection metrics on one of the data node and queries the
    // sqlite table on cluster_manager to check whether queue remediation actions has been
    // published.
    @Test
    @AExpect(
            what = AExpect.Type.DB_QUERY,
            on = HostTag.ELECTED_CLUSTER_MANAGER,
            validator = QueueDeciderValidator.class,
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
    public void testQueueCapacityDecider() {}
}
