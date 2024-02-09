/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.metric.temperature.byShard.calculators;

import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectSeekStep1;
import org.jooq.impl.DSL;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureDimension;

/**
 * This builds over the query from {@code SumOverOperationsForIndexShardGroup}. It calculates the
 * average over all index,shard groups.
 */
public class AvgShardBasedTemperatureCalculator extends ShardBasedTemperatureCalculator {
    public AvgShardBasedTemperatureCalculator(TemperatureDimension metricType) {
        super(metricType);
    }

    public static final String ALIAS = "sum_max";
    public static final String SHARD_AVG = "shard_avg";

    protected Field<?> getAggrDimension() {
        return super.getAggrDimension().as(ALIAS);
    }

    // This uses the return from the getSumOfUtilByIndexShardGroup as inner query and gets an
    // average over all index-shard groups.
    @Override
    protected Result<Record> createDslAndFetch(
            final DSLContext context,
            final String tableName,
            final Field<?> aggDimension,
            final List<Field<?>> groupByFieldsList,
            final List<Field<?>> selectFieldsList) {
        SelectSeekStep1<Record, ?> sumByIndexShardGroupsClause =
                getSumOfUtilByIndexShardGroup(
                        context, tableName, aggDimension, groupByFieldsList, selectFieldsList);

        selectFieldsList.clear();

        Field<?> avgOverShards = DSL.avg(DSL.field(DSL.name(ALIAS), Double.class)).as(SHARD_AVG);

        selectFieldsList.add(avgOverShards);
        Result<?> r = context.select(avgOverShards).from(sumByIndexShardGroupsClause).fetch();

        return (Result<Record>) r;
    }
}
