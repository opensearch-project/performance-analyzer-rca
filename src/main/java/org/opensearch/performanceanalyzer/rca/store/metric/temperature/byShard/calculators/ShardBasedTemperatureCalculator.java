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
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureDimension;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.TemperatureMetricsBase;

/**
 * This class gets all rows for the given metric table where the shardID is not NULL. This is to
 * tract resource utilization by shard ID. Shard on a node can be uniquely identified by the index
 * name and shard ID. It might also help to also consider the Operation dimension ?
 *
 * <p>This class calculates the sum over all operations for an index,shard pair.
 */
public class ShardBasedTemperatureCalculator extends TemperatureMetricsBase {
    private static final String[] dimensions = {
        AllMetrics.CommonDimension.INDEX_NAME.toString(),
        AllMetrics.CommonDimension.SHARD_ID.toString()
    };

    public ShardBasedTemperatureCalculator(TemperatureDimension metricType) {
        // The temperature intendeds to spread the temperature around the cluster by re-allocating
        // shards
        // from the hottest of nodes to the nodes that are relatively cold (with some randomness
        // so that it does not overwhelm the coldest node). Pyrometer also wants to size for peak
        // loads. Therefore, here we use MAX as the aggregate function.
        super(metricType, dimensions);
    }

    protected SelectSeekStep1<Record, ?> getSumOfUtilByIndexShardGroup(
            final DSLContext context,
            final String tableName,
            final Field<?> aggDimension,
            final List<Field<?>> groupByFieldsList,
            final List<Field<?>> selectFieldsList) {
        Field<?> shardIdField = DSL.field(DSL.name(AllMetrics.CommonDimension.SHARD_ID.toString()));
        // select indexName, shardId, sum(MetricsDB.max) from MetricsDB where ShardID is not null
        // group  by indexName, shardID.
        return context.select(selectFieldsList)
                .from(tableName)
                // as the goal is to aggregate by shardID, it cannot be null.
                .where(shardIdField.isNotNull())
                .groupBy(groupByFieldsList)
                .orderBy(aggDimension.desc());
    }

    @Override
    protected Result<Record> createDslAndFetch(
            final DSLContext context,
            final String tableName,
            final Field<?> aggDimension,
            final List<Field<?>> groupByFieldsList,
            final List<Field<?>> selectFieldsList) {
        return getSumOfUtilByIndexShardGroup(
                        context, tableName, aggDimension, groupByFieldsList, selectFieldsList)
                .fetch();
    }
}
