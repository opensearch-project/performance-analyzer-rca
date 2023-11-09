/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;


import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;

/*
 * SearchBackPressure cluster/node-level RCA would consume these data in the snapshots and determine whether the search back pressure service
 *  has cancelled too much/ too less requests, by comparing with predefined threshold.
 */
public class SearchBackPressureMetricsSnapShot implements Removable {

    // Logger for current class
    private static final Logger LOG = LogManager.getLogger(SearchBackPressureMetricsSnapShot.class);

    // entry point to interact with SQLite db
    private final DSLContext create;

    /*
     * This is a tmp table created to populate searchbp stats
     * table name is the search_back_pressure_ + windowStartTime
     */
    private final String tableName;

    /* columns are the key metrics to be collected (e.g. shar-level search back pressure cancellation count)
     */
    private List<Field<?>> columns;

    // Create a table with specifed fields (columns)
    public SearchBackPressureMetricsSnapShot(Connection conn, Long windowStartTime) {
        this.create = DSL.using(conn, SQLDialect.SQLITE);
        this.tableName = "search_back_pressure_" + windowStartTime;

        // Add the ControllerName, searchbp_mode columns in the table
        this.columns =
                new ArrayList<Field<?>>() {
                    {
                        // Shard/Task Stats Cancellation Count
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                AllMetrics.SearchBackPressureStatsValue
                                                        .SEARCHBP_SHARD_STATS_CANCELLATIONCOUNT
                                                        .toString()),
                                        Integer.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                AllMetrics.SearchBackPressureStatsValue
                                                        .SEARCHBP_TASK_STATS_CANCELLATIONCOUNT
                                                        .toString()),
                                        Integer.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                AllMetrics.SearchBackPressureStatsValue
                                                        .SEARCHBP_SHARD_STATS_COMPLETIONCOUNT
                                                        .toString())));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                AllMetrics.SearchBackPressureStatsValue
                                                        .SEARCHBP_TASK_STATS_COMPLETIONCOUNT
                                                        .toString())));

                        // Shard Stats Resource Heap / CPU Usage
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                AllMetrics.SearchBackPressureStatsValue
                                                        .SEARCHBP_SHARD_STATS_RESOURCE_HEAP_USAGE_CANCELLATIONCOUNT
                                                        .toString()),
                                        Integer.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                AllMetrics.SearchBackPressureStatsValue
                                                        .SEARCHBP_SHARD_STATS_RESOURCE_HEAP_USAGE_CURRENTMAX
                                                        .toString()),
                                        Integer.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                AllMetrics.SearchBackPressureStatsValue
                                                        .SEARCHBP_SHARD_STATS_RESOURCE_HEAP_USAGE_ROLLINGAVG
                                                        .toString()),
                                        Long.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                AllMetrics.SearchBackPressureStatsValue
                                                        .SEARCHBP_SHARD_STATS_RESOURCE_CPU_USAGE_CANCELLATIONCOUNT
                                                        .toString()),
                                        Integer.class));

                        this.add(
                                DSL.field(
                                        DSL.name(
                                                AllMetrics.SearchBackPressureStatsValue
                                                        .SEARCHBP_SHARD_STATS_RESOURCE_CPU_USAGE_CURRENTMAX
                                                        .toString()),
                                        Integer.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                AllMetrics.SearchBackPressureStatsValue
                                                        .SEARCHBP_SHARD_STATS_RESOURCE_CPU_USAGE_CURRENTAVG
                                                        .toString()),
                                        Long.class));

                        // Task Stats Resource Heap / CPU Usage
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                AllMetrics.SearchBackPressureStatsValue
                                                        .SEARCHBP_TASK_STATS_RESOURCE_HEAP_USAGE_CANCELLATIONCOUNT
                                                        .toString()),
                                        Integer.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                AllMetrics.SearchBackPressureStatsValue
                                                        .SEARCHBP_TASK_STATS_RESOURCE_HEAP_USAGE_CURRENTMAX
                                                        .toString()),
                                        Integer.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                AllMetrics.SearchBackPressureStatsValue
                                                        .SEARCHBP_TASK_STATS_RESOURCE_HEAP_USAGE_ROLLINGAVG
                                                        .toString()),
                                        Long.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                AllMetrics.SearchBackPressureStatsValue
                                                        .SEARCHBP_TASK_STATS_RESOURCE_CPU_USAGE_CANCELLATIONCOUNT
                                                        .toString()),
                                        Integer.class));

                        this.add(
                                DSL.field(
                                        DSL.name(
                                                AllMetrics.SearchBackPressureStatsValue
                                                        .SEARCHBP_TASK_STATS_RESOURCE_CPU_USAGE_CURRENTMAX
                                                        .toString()),
                                        Integer.class));
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                AllMetrics.SearchBackPressureStatsValue
                                                        .SEARCHBP_TASK_STATS_RESOURCE_CPU_USAGE_CURRENTAVG
                                                        .toString()),
                                        Long.class));
                    }
                };

        // create table with columns specified
        create.createTable(tableName).columns(columns).execute();
    }

    public DSLContext getDSLContext() {
        return create;
    }

    public BatchBindStep startBatchPut() {
        // Add dummy values because jooq requires this to support multiple bind statements with
        // single insert query
        List<Object> dummyValues = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            dummyValues.add(null);
        }
        return create.batch(create.insertInto(DSL.table(this.tableName)).values(dummyValues));
    }

    public Result<Record> fetchAll() {
        return create.select().from(DSL.table(tableName)).fetch();
    }

    @Override
    public void remove() throws Exception {
        create.dropTable(DSL.table(tableName)).execute();
    }
}
