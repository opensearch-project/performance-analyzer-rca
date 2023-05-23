/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;


import com.google.common.annotations.VisibleForTesting;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.SelectField;
import org.jooq.impl.DSL;
import org.opensearch.performanceanalyzer.DBUtils;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;

public class ClusterManagerThrottlingMetricsSnapshot implements Removable {

    private final DSLContext create;
    private final Long windowStartTime;
    private final String tableName;
    private List<Field<?>> columns;

    public ClusterManagerThrottlingMetricsSnapshot(Connection conn, Long windowStartTime) {
        this.create = DSL.using(conn, SQLDialect.SQLITE);
        this.windowStartTime = windowStartTime;
        this.tableName = "cluster_manager_throttling_" + windowStartTime;

        this.columns =
                new ArrayList<Field<?>>() {
                    {
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                AllMetrics.ClusterManagerThrottlingValue
                                                        .DATA_RETRYING_TASK_COUNT
                                                        .toString()),
                                        Long.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                AllMetrics.ClusterManagerThrottlingValue
                                                        .CLUSTER_MANAGER_THROTTLED_PENDING_TASK_COUNT
                                                        .toString()),
                                        Long.class));
                    }
                };
        create.createTable(this.tableName).columns(columns).execute();
    }

    @Override
    public void remove() throws Exception {
        create.dropTable(DSL.table(this.tableName)).execute();
    }

    /**
     * Return all cluster_manager throttling metric in the current window.
     *
     * <p>Actual Table Data_RetryingPendingTasksCount|ClusterManager_ThrottledPendingTasksCount 5
     * |10
     *
     * <p>
     *
     * @return aggregated cluster_manager task
     */
    public Result<Record> fetchAll() {
        return create.select().from(DSL.table(this.tableName)).fetch();
    }

    public BatchBindStep startBatchPut() {
        List<Object> dummyValues = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            dummyValues.add(null);
        }
        return create.batch(create.insertInto(DSL.table(this.tableName)).values(dummyValues));
    }

    /**
     * Return one row per cluster_manager throttling metric. It has 8 columns
     *
     * <p>|SUM_CLUSTER_MANAGER_THROTTLED_COUNT|AVG_CLUSTER_MANAGER_THROTTLED_COUNT|MIN_CLUSTER_MANAGER_THROTTLED_COUNT|MAX_CLUSTER_MANAGER_THROTTLED_COUNT|
     * SUM_DATA_RETRYING_COUNT|AVG_DATA_RETRYING_COUNT|MIN_DATA_RETRYING_COUNT|MAX_DATA_RETRYING_COUNT
     *
     * <p>
     *
     * @return aggregated cluster_manager task
     */
    public Result<Record> fetchAggregatedMetrics() {

        List<SelectField<?>> fields =
                new ArrayList<SelectField<?>>() {
                    {
                        this.add(
                                DSL.sum(
                                                DSL.field(
                                                        DSL.name(
                                                                AllMetrics
                                                                        .ClusterManagerThrottlingValue
                                                                        .CLUSTER_MANAGER_THROTTLED_PENDING_TASK_COUNT
                                                                        .toString()),
                                                        Long.class))
                                        .as(
                                                DBUtils.getAggFieldName(
                                                        AllMetrics.ClusterManagerThrottlingValue
                                                                .CLUSTER_MANAGER_THROTTLED_PENDING_TASK_COUNT
                                                                .toString(),
                                                        MetricsDB.SUM)));
                        this.add(
                                DSL.avg(
                                                DSL.field(
                                                        DSL.name(
                                                                AllMetrics
                                                                        .ClusterManagerThrottlingValue
                                                                        .CLUSTER_MANAGER_THROTTLED_PENDING_TASK_COUNT
                                                                        .toString()),
                                                        Double.class))
                                        .as(
                                                DBUtils.getAggFieldName(
                                                        AllMetrics.ClusterManagerThrottlingValue
                                                                .CLUSTER_MANAGER_THROTTLED_PENDING_TASK_COUNT
                                                                .toString(),
                                                        MetricsDB.AVG)));
                        this.add(
                                DSL.min(
                                                DSL.field(
                                                        DSL.name(
                                                                AllMetrics
                                                                        .ClusterManagerThrottlingValue
                                                                        .CLUSTER_MANAGER_THROTTLED_PENDING_TASK_COUNT
                                                                        .toString()),
                                                        Long.class))
                                        .as(
                                                DBUtils.getAggFieldName(
                                                        AllMetrics.ClusterManagerThrottlingValue
                                                                .CLUSTER_MANAGER_THROTTLED_PENDING_TASK_COUNT
                                                                .toString(),
                                                        MetricsDB.MIN)));
                        this.add(
                                DSL.max(
                                                DSL.field(
                                                        DSL.name(
                                                                AllMetrics
                                                                        .ClusterManagerThrottlingValue
                                                                        .CLUSTER_MANAGER_THROTTLED_PENDING_TASK_COUNT
                                                                        .toString()),
                                                        Long.class))
                                        .as(
                                                DBUtils.getAggFieldName(
                                                        AllMetrics.ClusterManagerThrottlingValue
                                                                .CLUSTER_MANAGER_THROTTLED_PENDING_TASK_COUNT
                                                                .toString(),
                                                        MetricsDB.MAX)));

                        this.add(
                                DSL.sum(
                                                DSL.field(
                                                        DSL.name(
                                                                AllMetrics
                                                                        .ClusterManagerThrottlingValue
                                                                        .DATA_RETRYING_TASK_COUNT
                                                                        .toString()),
                                                        Long.class))
                                        .as(
                                                DBUtils.getAggFieldName(
                                                        AllMetrics.ClusterManagerThrottlingValue
                                                                .DATA_RETRYING_TASK_COUNT
                                                                .toString(),
                                                        MetricsDB.SUM)));
                        this.add(
                                DSL.avg(
                                                DSL.field(
                                                        DSL.name(
                                                                AllMetrics
                                                                        .ClusterManagerThrottlingValue
                                                                        .DATA_RETRYING_TASK_COUNT
                                                                        .toString()),
                                                        Double.class))
                                        .as(
                                                DBUtils.getAggFieldName(
                                                        AllMetrics.ClusterManagerThrottlingValue
                                                                .DATA_RETRYING_TASK_COUNT
                                                                .toString(),
                                                        MetricsDB.AVG)));
                        this.add(
                                DSL.min(
                                                DSL.field(
                                                        DSL.name(
                                                                AllMetrics
                                                                        .ClusterManagerThrottlingValue
                                                                        .DATA_RETRYING_TASK_COUNT
                                                                        .toString()),
                                                        Long.class))
                                        .as(
                                                DBUtils.getAggFieldName(
                                                        AllMetrics.ClusterManagerThrottlingValue
                                                                .DATA_RETRYING_TASK_COUNT
                                                                .toString(),
                                                        MetricsDB.MIN)));
                        this.add(
                                DSL.max(
                                                DSL.field(
                                                        DSL.name(
                                                                AllMetrics
                                                                        .ClusterManagerThrottlingValue
                                                                        .DATA_RETRYING_TASK_COUNT
                                                                        .toString()),
                                                        Long.class))
                                        .as(
                                                DBUtils.getAggFieldName(
                                                        AllMetrics.ClusterManagerThrottlingValue
                                                                .DATA_RETRYING_TASK_COUNT
                                                                .toString(),
                                                        MetricsDB.MAX)));
                    }
                };
        ArrayList<Field<?>> groupByFields = new ArrayList<Field<?>>();

        return create.select(fields).from(DSL.table(this.tableName)).groupBy(groupByFields).fetch();
    }

    @VisibleForTesting
    public void putMetrics(long retrying_task, Map<String, String> dimensions) {
        Map<Field<?>, String> dimensionMap = new HashMap<>();
        for (Map.Entry<String, String> dimension : dimensions.entrySet()) {
            dimensionMap.put(
                    DSL.field(DSL.name(dimension.getKey()), String.class), dimension.getValue());
        }
        create.insertInto(DSL.table(this.tableName))
                .set(
                        DSL.field(
                                DSL.name(
                                        AllMetrics.ClusterManagerThrottlingValue
                                                .DATA_RETRYING_TASK_COUNT
                                                .toString()),
                                Long.class),
                        retrying_task)
                .set(
                        DSL.field(
                                DSL.name(
                                        AllMetrics.ClusterManagerThrottlingValue
                                                .CLUSTER_MANAGER_THROTTLED_PENDING_TASK_COUNT
                                                .toString()),
                                Long.class),
                        0L)
                .set(dimensionMap)
                .execute();
    }
}
