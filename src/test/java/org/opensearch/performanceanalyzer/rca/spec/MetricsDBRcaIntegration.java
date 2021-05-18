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
 * Copyright 2019-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package org.opensearch.performanceanalyzer.rca.spec;


import java.util.ArrayList;
import java.util.List;
import org.jooq.Record;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.GradleTaskForRca;
import org.opensearch.performanceanalyzer.rca.framework.core.Queryable;
import org.opensearch.performanceanalyzer.rca.spec.helpers.AssertHelper;

@Category(GradleTaskForRca.class)
public class MetricsDBRcaIntegration {
    @Test
    public void testIntegration() throws Exception {
        Queryable queryable = new MetricsDBProviderTestHelper();
        List<List<String>> expectedReturn = new ArrayList<>();
        List<String> cols =
                new ArrayList<String>() {
                    {
                        this.add("ShardID");
                        this.add("IndexName");
                        this.add("Operation");
                        this.add("ShardRole");
                        this.add("sum");
                        this.add("avg");
                        this.add("min");
                        this.add("max");
                    }
                };
        List<String> row1 =
                new ArrayList<String>() {
                    {
                        this.add("CPU_UtilizationShardID");
                        this.add("CPU_UtilizationIndexName");
                        this.add("CPU_UtilizationOperation");
                        this.add("CPU_UtilizationShardRole");
                        this.add("1.0");
                        this.add("1.0");
                        this.add("1.0");
                        this.add("1.0");
                    }
                };

        expectedReturn.add(cols);
        expectedReturn.add(row1);

        MetricsDB db = queryable.getMetricsDB();
        for (Record record :
                queryable.queryMetrics(db, AllMetrics.OSMetrics.CPU_UTILIZATION.toString())) {
            AssertHelper.compareRecord(cols, row1, record);
        }
    }
}
