/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.summaries.temperature;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.GeneratedMessageV3;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureDimension;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureVector;
import org.opensearch.performanceanalyzer.rca.framework.util.SQLiteQueryUtils;

/**
 * This is the temperature profile of a cluster. It encapsulates all the summaries for all the
 * tracked dimensions.
 */
public class ClusterTemperatureSummary extends GenericSummary {
    public static final String TABLE_NAME = ClusterTemperatureSummary.class.getSimpleName();
    public static final String NUM_NODES = "num_nodes";

    private final ClusterDimensionalSummary[] nodeDimensionalTemperatureSummaries;

    /** This is the summary of all the nodes in the cluster. */
    private final CompactClusterLevelNodeSummary[] nodes;

    private int numberOfNodes;

    public ClusterTemperatureSummary(int numberOfNodes) {
        this.numberOfNodes = numberOfNodes;
        this.nodeDimensionalTemperatureSummaries =
                new ClusterDimensionalSummary[TemperatureDimension.values().length];
        this.nodes = new CompactClusterLevelNodeSummary[this.numberOfNodes];
    }

    public void createClusterDimensionalTemperature(
            TemperatureDimension dimension,
            TemperatureVector.NormalizedValue mean,
            double totalUsage) {
        ClusterDimensionalSummary summary = new ClusterDimensionalSummary(dimension);
        summary.setMeanTemperature(mean);
        summary.setTotalUsage(totalUsage);
        nodeDimensionalTemperatureSummaries[dimension.ordinal()] = summary;
    }

    // The String key is the node ID.
    public void addNodesSummaries(
            Map<String, CompactClusterLevelNodeSummary> nodeTemperatureSummaries) {
        int i = 0;
        for (CompactClusterLevelNodeSummary nodeTemperatureSummaryVal :
                nodeTemperatureSummaries.values()) {
            for (TemperatureDimension dimension : TemperatureDimension.values()) {
                nodeDimensionalTemperatureSummaries[dimension.ordinal()].addNodeToZone(
                        nodeTemperatureSummaryVal);
            }
            nodes[i] = nodeTemperatureSummaryVal;
            i++;
        }
    }

    public void addNodeSummaries(List<CompactClusterLevelNodeSummary> nodeSummaries) {
        int i = 0;
        for (CompactClusterLevelNodeSummary nodeSummary : nodeSummaries) {
            nodes[i] = nodeSummary;
            i++;
        }
    }

    public List<GenericSummary> getNestedSummaryList() {
        List<GenericSummary> summaries = new ArrayList<>();

        for (GenericSummary summary : nodeDimensionalTemperatureSummaries) {
            summaries.add(summary);
        }

        for (GenericSummary summary : nodes) {
            summaries.add(summary);
        }
        return summaries;
    }

    @Override
    public <T extends GeneratedMessageV3> T buildSummaryMessage() {
        throw new IllegalArgumentException();
    }

    @Override
    public void buildSummaryMessageAndAddToFlowUnit(FlowUnitMessage.Builder messageBuilder) {
        throw new IllegalArgumentException();
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    private static List<Field<?>> getColumns() {
        List<Field<?>> schema = new ArrayList<>();
        schema.add(DSL.field(DSL.name(NUM_NODES), Short.class));
        return schema;
    }

    @Override
    public List<Field<?>> getSqlSchema() {
        return getColumns();
    }

    @Override
    public List<Object> getSqlValue() {
        List<Object> values = new ArrayList<>();
        values.add(numberOfNodes);
        return values;
    }

    @Override
    public JsonElement toJson() {
        JsonObject summaryObj = new JsonObject();

        JsonArray dimTempArray = new JsonArray();
        for (ClusterDimensionalSummary summary : nodeDimensionalTemperatureSummaries) {
            dimTempArray.add(summary.toJson());
        }
        if (dimTempArray.size() > 0) {
            summaryObj.add(ClusterDimensionalSummary.TABLE_NAME, dimTempArray);
        }

        JsonArray nodesArr = new JsonArray();
        String elemName = "";
        for (CompactClusterLevelNodeSummary nodeSummary : nodes) {
            if (nodeSummary != null) {
                nodesArr.add(nodeSummary.toJson());
                elemName = nodeSummary.TABLE_NAME;
            }
        }
        if (!elemName.isEmpty()) {
            summaryObj.add(elemName, nodesArr);
        }
        return summaryObj;
    }

    /**
     * The ClusterTemperatureSummary is stored in the SQLite file as three columns:
     * ClusterTemperatureSummary_ID | num_nodes | RCA_ID
     *
     * <p>where column 1 is the primary key of this table. The RCA_ID ties this instance of summary
     * to an an instance of ClusterTemperatureRCA.
     *
     * @param records The table of records. It should only contain one row for a given RCA_ID.
     * @param context The Database connection context.
     * @return The Summary object with other nested objects.
     */
    public static ClusterTemperatureSummary buildSummaryFromDatabase(
            Result<Record> records, DSLContext context) {
        int numNodes = -1;

        if (records.size() > 1) {
            throw new IllegalArgumentException(
                    "Only 1 ClusterTemperatureSummary expected." + records);
        }

        Record record = records.get(0);
        for (Field<?> field : getColumns()) {
            numNodes = record.get(field, Integer.class);
        }
        ClusterTemperatureSummary summary = new ClusterTemperatureSummary(numNodes);

        String dimensionalTablename = ClusterDimensionalSummary.TABLE_NAME;
        int clusterTemperatureID =
                record.get(SQLiteQueryUtils.getPrimaryKeyColumnName(TABLE_NAME), Integer.class);

        Field<Integer> foreignKeyForDimensionalTable =
                DSL.field(SQLiteQueryUtils.getPrimaryKeyColumnName(TABLE_NAME), Integer.class);

        SelectJoinStep<Record> rcaQuery =
                SQLiteQueryUtils.buildSummaryQuery(
                        context,
                        dimensionalTablename,
                        clusterTemperatureID,
                        foreignKeyForDimensionalTable);

        Result<Record> recordList = rcaQuery.fetch();

        for (Record record1 : recordList) {
            ClusterDimensionalSummary dimSummary =
                    ClusterDimensionalSummary.build(record1, context);

            summary.nodeDimensionalTemperatureSummaries[
                            dimSummary.getProfileForDimension().ordinal()] =
                    dimSummary;
        }

        rcaQuery =
                SQLiteQueryUtils.buildSummaryQuery(
                        context,
                        CompactClusterLevelNodeSummary.class.getSimpleName(),
                        clusterTemperatureID,
                        foreignKeyForDimensionalTable);

        recordList = rcaQuery.fetch();
        List<CompactClusterLevelNodeSummary> nodeSummaries = new ArrayList<>();
        for (Record record1 : recordList) {
            CompactClusterLevelNodeSummary nodeSummary =
                    CompactClusterLevelNodeSummary.build(record1);
            nodeSummaries.add(nodeSummary);
        }
        summary.addNodeSummaries(nodeSummaries);
        return summary;
    }
}
