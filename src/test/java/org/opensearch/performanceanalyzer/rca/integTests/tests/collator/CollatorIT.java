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

package org.opensearch.performanceanalyzer.rca.integTests.tests.collator;


import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.GC_Collection_Event;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.GC_Type;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Heap_Max;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Heap_Used;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.ThreadPool_QueueCapacity;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.ThreadPool_RejectedReqs;
import org.opensearch.performanceanalyzer.rca.integTests.framework.RcaItMarker;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AClusterType;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AExpect;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AMetric;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ARcaConf;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ARcaGraph;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ATable;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.ATuple;
import org.opensearch.performanceanalyzer.rca.integTests.framework.configs.ClusterType;
import org.opensearch.performanceanalyzer.rca.integTests.framework.configs.HostTag;
import org.opensearch.performanceanalyzer.rca.integTests.framework.runners.RcaItNotEncryptedRunner;
import org.opensearch.performanceanalyzer.rca.integTests.tests.collator.validator.CollatorValidator;
import org.opensearch.performanceanalyzer.rca.integTests.tests.jvmsizing.JvmSizingITConstants;
import org.opensearch.performanceanalyzer.rca.persistence.actions.PersistedAction;
import org.opensearch.performanceanalyzer.rca.store.OpenSearchAnalysisGraph;

@Category(RcaItMarker.class)
@RunWith(RcaItNotEncryptedRunner.class)
@AClusterType(ClusterType.MULTI_NODE_CO_LOCATED_MASTER)
@ARcaGraph(OpenSearchAnalysisGraph.class)
@ARcaConf(
        dataNode = CollatorITConstants.RCA_CONF_PATH + "rca.conf",
        electedMaster = CollatorITConstants.RCA_CONF_PATH + "rca_master.conf")
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
                    hostTag = HostTag.ELECTED_MASTER,
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
                    hostTag = HostTag.ELECTED_MASTER,
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
                    hostTag = HostTag.ELECTED_MASTER,
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
                    hostTag = HostTag.ELECTED_MASTER,
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
public class CollatorIT {

    @Test
    @AExpect(
            what = AExpect.Type.REST_API,
            on = HostTag.ELECTED_MASTER,
            validator = CollatorValidator.class,
            forRca = PersistedAction.class,
            timeoutSeconds = 190)
    public void testCollatorMisaligned() {}
}
