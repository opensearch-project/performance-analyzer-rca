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

public class ShardStateMetricsSnapshot implements Removable {
    private static final Logger LOG = LogManager.getLogger(ShardStateMetricsSnapshot.class);
    private final DSLContext create;
    private final String tableName;
    private static final List<Field<?>> columns =
            new ArrayList<Field<?>>() {
                {
                    this.add(
                            DSL.field(
                                    DSL.name(AllMetrics.ShardStateDimension.INDEX_NAME.toString()),
                                    String.class));
                    this.add(
                            DSL.field(
                                    DSL.name(AllMetrics.ShardStateDimension.SHARD_ID.toString()),
                                    String.class));
                    this.add(
                            DSL.field(
                                    DSL.name(AllMetrics.ShardStateDimension.SHARD_TYPE.toString()),
                                    String.class));
                    this.add(
                            DSL.field(
                                    DSL.name(AllMetrics.ShardStateDimension.NODE_NAME.toString()),
                                    String.class));
                    this.add(
                            DSL.field(
                                    DSL.name(AllMetrics.ShardStateDimension.SHARD_STATE.toString()),
                                    String.class));
                }
            };

    public ShardStateMetricsSnapshot(Connection conn, Long windowStartTime) {
        this.create = DSL.using(conn, SQLDialect.SQLITE);
        this.tableName = "shard_state_" + windowStartTime;
        create.createTable(this.tableName).columns(columns).execute();
    }

    @Override
    public void remove() throws Exception {
        create.dropTable(DSL.table(this.tableName)).execute();
    }

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

    @VisibleForTesting
    public void putMetrics(String shard_state, Map<String, String> dimensions) {
        Map<Field<?>, String> dimensionMap = new HashMap<>();
        for (Map.Entry<String, String> dimension : dimensions.entrySet()) {
            dimensionMap.put(
                    DSL.field(DSL.name(dimension.getKey()), String.class), dimension.getValue());
        }
        create.insertInto(DSL.table(this.tableName))
                .set(
                        DSL.field(
                                DSL.name(AllMetrics.ShardStateDimension.SHARD_STATE.toString()),
                                String.class),
                        shard_state)
                .set(dimensionMap)
                .execute();
    }
}
