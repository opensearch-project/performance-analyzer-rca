/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.spec.helpers;


import java.util.ArrayList;
import java.util.List;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metricsdb.Dimensions;
import org.opensearch.performanceanalyzer.metricsdb.Metric;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;

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
