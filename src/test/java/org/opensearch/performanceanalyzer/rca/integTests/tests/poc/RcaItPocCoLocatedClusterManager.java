/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.tests.poc;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.CPU_Utilization;
import org.opensearch.performanceanalyzer.rca.integTests.framework.RcaItMarker;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AClusterType;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AExpect;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AMetric;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ARcaGraph;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ATable;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ATuple;
import org.opensearch.performanceanalyzer.rca.integTests.framework.api.TestApi;
import org.opensearch.performanceanalyzer.rca.integTests.framework.configs.ClusterType;
import org.opensearch.performanceanalyzer.rca.integTests.framework.configs.HostTag;
import org.opensearch.performanceanalyzer.rca.integTests.framework.runners.RcaItNotEncryptedRunner;
import org.opensearch.performanceanalyzer.rca.integTests.tests.poc.validator.PocValidator;

@RunWith(RcaItNotEncryptedRunner.class)
@Category(RcaItMarker.class)
@AClusterType(ClusterType.MULTI_NODE_CO_LOCATED_CLUSTER_MANAGER)
@ARcaGraph(RcaItPocSingleNode.SimpleAnalysisGraphForCoLocated.class)
@AMetric(
        name = CPU_Utilization.class,
        dimensionNames = {
            AllMetrics.CommonDimension.Constants.SHARDID_VALUE,
            AllMetrics.CommonDimension.Constants.INDEX_NAME_VALUE,
            AllMetrics.CommonDimension.Constants.OPERATION_VALUE,
            AllMetrics.CommonDimension.Constants.SHARD_ROLE_VALUE
        },
        tables = {
            @ATable(
                    hostTag = HostTag.DATA_0,
                    tuple = {
                        @ATuple(
                                dimensionValues = {"0", "logs", "bulk", "p"},
                                sum = 0.0,
                                avg = 0.0,
                                min = 0.0,
                                max = 0.0),
                        @ATuple(
                                dimensionValues = {"1", "logs", "bulk", "r"},
                                sum = 0.0,
                                avg = 0.0,
                                min = 0.0,
                                max = 80.0),
                        @ATuple(
                                dimensionValues = {"2", "logs", "bulk", "p"},
                                sum = 0.0,
                                avg = 0.0,
                                min = 0.0,
                                max = 10.0)
                    }),
            @ATable(
                    hostTag = {HostTag.ELECTED_CLUSTER_MANAGER},
                    tuple = {
                        @ATuple(
                                dimensionValues = {"0", "logs", "bulk", "r"},
                                sum = 0.0,
                                avg = 0.0,
                                min = 0.0,
                                max = 50.0),
                        @ATuple(
                                dimensionValues = {"1", "logs", "bulk", "p"},
                                sum = 0.0,
                                avg = 0.0,
                                min = 0.0,
                                max = 5.0),
                        @ATuple(
                                dimensionValues = {"2", "logs", "bulk", "r"},
                                sum = 0.0,
                                avg = 0.0,
                                min = 0.0,
                                max = 11.0)
                    })
        })
public class RcaItPocCoLocatedClusterManager {
    private TestApi api;

    @Test
    @AExpect(
            what = AExpect.Type.REST_API,
            on = HostTag.ELECTED_CLUSTER_MANAGER,
            validator = PocValidator.class,
            forRca = RcaItPocSingleNode.SimpleAnalysisGraphForCoLocated.ClusterRca.class)
    public void simple() {}

    public void setTestApi(final TestApi api) {
        this.api = api;
    }
}
