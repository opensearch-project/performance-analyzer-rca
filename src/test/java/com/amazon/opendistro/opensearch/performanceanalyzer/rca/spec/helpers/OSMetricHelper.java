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

package com.amazon.opendistro.opensearch.performanceanalyzer.rca.spec.helpers;


import com.amazon.opendistro.opensearch.performanceanalyzer.metrics.AllMetrics;
import com.amazon.opendistro.opensearch.performanceanalyzer.metricsdb.Dimensions;
import com.amazon.opendistro.opensearch.performanceanalyzer.metricsdb.Metric;
import com.amazon.opendistro.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import java.util.ArrayList;
import java.util.List;

public class OSMetricHelper {
    static List<String> dims;

    static {
        dims =
                new ArrayList<String>() {
                    {
                        this.add(AllMetrics.CommonDimension.SHARD_ID.toString());
                        this.add(AllMetrics.CommonDimension.INDEX_NAME.toString());
                        this.add(AllMetrics.CommonDimension.OPERATION.toString());
                        this.add(AllMetrics.CommonDimension.SHARD_ROLE.toString());
                    }
                };
    }

    public static List<String> getDims() {
        return dims;
    }

    public static void create(MetricsDB metricsDB, String metric) {
        metricsDB.createMetric(new Metric<>(metric, 0d), dims);
    }

    public static void insert(MetricsDB metricsDB, String metricName, double value) {
        Dimensions dimensions = new Dimensions();
        String shardIdColName = dims.get(0);
        String idxColName = dims.get(1);
        String opColName = dims.get(2);
        String shardRoleColName = dims.get(3);
        dimensions.put(shardIdColName, metricName + shardIdColName);
        dimensions.put(idxColName, metricName + idxColName);
        dimensions.put(opColName, metricName + opColName);
        dimensions.put(shardRoleColName, metricName + shardRoleColName);

        metricsDB.putMetric(new Metric<>(metricName, value), dimensions, 0);
    }

    /**
     * Insert one full column with all custom values.
     *
     * @param metricsDB The database to enter it in.
     * @param metricName The name of the table in the metricsDB
     * @param value The value of the metric.
     * @param dimVales The value of the dimensions. The dimensions are interpreted in this order:
     *     shardID, index name, operation value and shard role.
     */
    public static void insert(
            MetricsDB metricsDB, String metricName, double value, List<String> dimVales) {
        Dimensions dimensions = new Dimensions();
        for (int i = 0; i < dims.size(); i++) {
            dimensions.put(dims.get(i), dimVales.get(i));
        }

        metricsDB.putMetric(new Metric<>(metricName, value), dimensions, 0);
    }
}
