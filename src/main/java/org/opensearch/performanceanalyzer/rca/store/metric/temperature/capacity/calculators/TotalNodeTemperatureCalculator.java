/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.metric.temperature.capacity.calculators;


import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureDimension;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.TemperatureMetricsBase;

/**
 * This class gets all rows for the given metric table where the shardID is not NULL. This is to
 * tract resource utilization by shard ID. Shard on a node can be uniquely identified by the index
 * name and shard ID. It might also help to also consider the Operation dimension ?
 */
public class TotalNodeTemperatureCalculator extends TemperatureMetricsBase {
    // For peak usage there is no group by clause used, therefore this is empty.
    private static final String[] dimensions = {};

    public TotalNodeTemperatureCalculator(TemperatureDimension metricType) {
        super(metricType, dimensions);
    }

    @Override
    protected Result<Record> createDslAndFetch(
            final DSLContext context,
            final String tableName,
            final Field<?> aggDimension,
            final List<Field<?>> groupByFieldsList,
            final List<Field<?>> selectFieldsList) {

        Result<?> r1 = context.select(selectFieldsList).from(tableName).fetch();
        return (Result<Record>) r1;
    }

    @Override
    protected List<Field<?>> getSelectFieldsList(
            final List<Field<?>> groupByFields, Field<?> aggrDimension) {
        return aggrColumnAsSelectField();
    }
}
