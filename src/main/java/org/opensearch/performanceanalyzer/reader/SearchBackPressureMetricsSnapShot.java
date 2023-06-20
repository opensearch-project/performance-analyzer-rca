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

public class SearchBackPressureMetricsSnapShot implements Removable {

    // Logger for current class
    private static final Logger LOG = LogManager.getLogger(SearchBackPressureMetricsSnapShot.class);

    // entry point to interact with SQLite db
    private final DSLContext create;

    private final String tableName;
    private List<Field<?>> columns;

    // Global variables for naming
    private static final String SEARCHBP_CONTROLLER_NAME_VALUE = "ControllerName";
    private static final String SEARCHBP_MODE_VALUE = "searchbp_mode";

    // Create a table with specifed fields (columns)
    public SearchBackPressureMetricsSnapShot(Connection conn, Long windowStartTime) {
        this.create = DSL.using(conn, SQLDialect.SQLITE);
        this.tableName = "search_back_pressure_" + windowStartTime;

        // Add the ControllerName, searchbp_mode columns in the table
        this.columns =
                new ArrayList<Field<?>>() {
                    {
                        this.add(
                                DSL.field(
                                        DSL.name(
                                                AllMetrics.SearchBackPressureStatsValue
                                                        .SEARCHBP_SHARD_STATS_CANCELLATIONCOUNT
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

    public enum Fields {
        CONTROLLER_NAME(SEARCHBP_CONTROLLER_NAME_VALUE),
        SEARCHBP_MODE(SEARCHBP_MODE_VALUE);

        private final String fieldValue;

        Fields(String fieldValue) {
            this.fieldValue = fieldValue;
        }

        @Override
        public String toString() {
            return fieldValue;
        }
    }
}
