/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.util;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit.ResourceFlowUnitFieldValue;
import org.opensearch.performanceanalyzer.rca.store.rca.HighHeapUsageClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.HotNodeClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.FieldDataCacheClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.QueueRejectionClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.ShardRequestCacheClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.hotshard.HotShardClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.temperature.ClusterTemperatureRca;
import org.opensearch.performanceanalyzer.rca.store.rca.temperature.NodeTemperatureRca;
import org.opensearch.performanceanalyzer.rca.store.rca.temperature.dimension.CpuUtilDimensionTemperatureRca;
import org.opensearch.performanceanalyzer.rca.store.rca.temperature.dimension.HeapAllocRateTemperatureRca;
import org.opensearch.performanceanalyzer.rca.store.rca.temperature.dimension.ShardSizeDimensionTemperatureRca;

/** A utility class to query cluster, node and resource level summary for a rca */
public class SQLiteQueryUtils {
    private static final Set<String> clusterLevelRCA;
    private static final Set<String> temperatureProfileRCASet;

    public static final Set<String> temperatureProfileDimensionRCASet;

    public static final String ALL_TEMPERATURE_DIMENSIONS = "AllTemperatureDimensions";

    // RCAs that can be queried by RCA API
    // currently we can only query from the cluster level RCAs
    static {
        Set<String> rcaSet = new HashSet<>();

        rcaSet.add(ClusterTemperatureRca.TABLE_NAME);
        rcaSet.add(HighHeapUsageClusterRca.RCA_TABLE_NAME);
        rcaSet.add(HotNodeClusterRca.RCA_TABLE_NAME);
        rcaSet.add(HotShardClusterRca.RCA_TABLE_NAME);
        rcaSet.add(QueueRejectionClusterRca.RCA_TABLE_NAME);
        rcaSet.add(FieldDataCacheClusterRca.RCA_TABLE_NAME);
        rcaSet.add(ShardRequestCacheClusterRca.RCA_TABLE_NAME);
        clusterLevelRCA = Collections.unmodifiableSet(rcaSet);
    }

    // Temperature profile RCAs that can be queried by the RCA API.
    static {
        Set<String> temperatureDimensions = new HashSet<>();
        temperatureDimensions.add(CpuUtilDimensionTemperatureRca.class.getSimpleName());
        temperatureDimensions.add(HeapAllocRateTemperatureRca.class.getSimpleName());
        temperatureDimensions.add(ShardSizeDimensionTemperatureRca.class.getSimpleName());

        temperatureProfileDimensionRCASet = Collections.unmodifiableSet(temperatureDimensions);

        Set<String> tempProfileRcaSet = new HashSet<>();

        tempProfileRcaSet.addAll(temperatureProfileDimensionRCASet);

        tempProfileRcaSet.add(ALL_TEMPERATURE_DIMENSIONS);
        tempProfileRcaSet.add(NodeTemperatureRca.TABLE_NAME);
        tempProfileRcaSet.add(ClusterTemperatureRca.TABLE_NAME);
        temperatureProfileRCASet = Collections.unmodifiableSet(tempProfileRcaSet);
    }

    /**
     * get a list of all the RCAs that are supported by RCA API.
     *
     * @return list of supported RCAs
     */
    public static List<String> getClusterLevelRca() {
        return ImmutableList.copyOf(clusterLevelRCA);
    }

    /**
     * check if the rca is a cluster level rca
     *
     * @param rca the name of rca
     * @return if it is a cluster level rca
     */
    public static boolean isClusterLevelRca(String rca) {
        if (rca == null) {
            return false;
        }
        return clusterLevelRCA.contains(rca);
    }

    /**
     * This function build SQL query to fetch a rca from RCA table
     *
     * @param ctx DSLContext
     * @param rca The rca that will be queried
     * @return jooq query object
     */
    public static SelectJoinStep<Record> buildRcaQuery(final DSLContext ctx, final String rca) {
        SelectJoinStep<Record> rcaQuery = ctx.select().from(ResourceFlowUnit.RCA_TABLE_NAME);
        rcaQuery.where(
                        DSL.field(
                                        ResourceFlowUnitFieldValue.RCA_NAME_FILELD.getName(),
                                        String.class)
                                .equal(rca))
                .orderBy(ResourceFlowUnitFieldValue.TIMESTAMP_FIELD.getField().desc());
        return rcaQuery;
    }

    /**
     * This function build SQL query to fetch summary from a summay table
     *
     * @param ctx DSLContext
     * @param tableName the summary table to query from
     * @param foreignKey the foreign used in where clause
     * @param foreignKeyField the foriegn key field in this table
     * @return jooq query object
     */
    public static SelectJoinStep<Record> buildSummaryQuery(
            final DSLContext ctx,
            final String tableName,
            final int foreignKey,
            final Field<Integer> foreignKeyField) {
        SelectJoinStep<Record> summaryQuery = ctx.select().from(tableName);
        summaryQuery.where(foreignKeyField.equal(foreignKey));
        return summaryQuery;
    }

    /**
     * generate the name of primary key field in each table
     *
     * @param tableName table name
     * @return primary key field name
     */
    public static String getPrimaryKeyColumnName(String tableName) {
        return tableName + "_ID";
    }

    public static List<String> getTemperatureProfileRcas() {
        return ImmutableList.copyOf(temperatureProfileRCASet);
    }

    public static boolean isTemperatureProfileRca(String rca) {
        if (rca == null) {
            return false;
        }

        return temperatureProfileRCASet.contains(rca);
    }
}
