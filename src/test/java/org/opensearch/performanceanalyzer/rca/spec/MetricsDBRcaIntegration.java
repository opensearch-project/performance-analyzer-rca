/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.spec;

import java.util.ArrayList;
import java.util.List;
import org.jooq.Record;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
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
