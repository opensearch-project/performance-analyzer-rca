/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.util;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.framework.core.Queryable;
import org.opensearch.performanceanalyzer.reader.Removable;

public class SQLiteReader implements Queryable, Removable {
    private final Connection conn;
    private final DSLContext dslContext;
    private final String DBProtocol = "jdbc:sqlite:";

    public SQLiteReader(final String pathToSqlite) throws SQLException {
        conn = DriverManager.getConnection(DBProtocol + pathToSqlite);
        dslContext = DSL.using(conn, SQLDialect.SQLITE);
    }

    public DSLContext getContext() {
        return dslContext;
    }

    @Override
    public void remove() throws Exception {
        conn.close();
    }

    @Override
    public MetricsDB getMetricsDB() throws Exception {
        return new MetricsDBX(System.currentTimeMillis(), getContext());
    }

    @Override
    public Result<Record> queryMetrics(MetricsDB db, String metricName) {
        Result<Record> result = getContext().select().from(DSL.table(metricName)).fetch();
        return result;
    }

    @Override
    public Result<Record> queryMetrics(
            MetricsDB db, String metricName, String dimension, String aggregation) {
        throw new IllegalArgumentException("Should not call");
    }

    @Override
    public long getDBTimestamp(MetricsDB db) {
        return 0;
    }

    private static class MetricsDBX extends MetricsDB {
        private final DSLContext dslContext;

        public MetricsDBX(long windowStartTime, final DSLContext dslContext) throws Exception {
            super(windowStartTime);
            this.dslContext = dslContext;
        }

        @Override
        public DSLContext getDSLContext() {
            return dslContext;
        }
    }
}
