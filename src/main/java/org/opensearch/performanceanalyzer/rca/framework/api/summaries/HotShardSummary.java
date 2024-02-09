/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.summaries;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.exception.DataTypeException;
import org.jooq.impl.DSL;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.grpc.HotShardSummaryMessage;
import org.opensearch.performanceanalyzer.grpc.HotShardSummaryMessage.CriteriaEnum;
import org.opensearch.performanceanalyzer.rca.framework.api.persist.JooqFieldValue;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericSummary;

/**
 * HotShardSummary contains information such as the index_name, shard_id, node_id, cpu_utilization,
 * criteria and time_period.
 *
 * <p>The hot shard summary is created by node level and cluster level RCAs running on data nodes
 * and elected cluster_manager node resp. This object is persisted in SQLite table. Table name :
 * HotClusterSummary
 *
 * <p>schema : | ID(primary key) | index_name | shard_id | node_id | cpu_utilization | ID in
 * FlowUnit(foreign key)
 */
public class HotShardSummary extends GenericSummary {

    public static final String HOT_SHARD_SUMMARY_TABLE = HotShardSummary.class.getSimpleName();
    private static final Logger LOG = LogManager.getLogger(HotShardSummary.class);
    private final String indexName;
    private final String shardId;
    private final String nodeId;
    private CriteriaEnum criteria;
    private double cpuUtilization;
    private int timePeriodInSeconds;

    public HotShardSummary(String indexName, String shardId, String nodeId, int timePeriod) {
        super();
        this.indexName = indexName;
        this.shardId = shardId;
        this.nodeId = nodeId;
        this.timePeriodInSeconds = timePeriod;
    }

    public void setCpuUtilization(final double cpuUtilization) {
        this.cpuUtilization = cpuUtilization;
    }

    public void setCriteria(final CriteriaEnum criteria) {
        this.criteria = criteria;
    }

    public String getIndexName() {
        return this.indexName;
    }

    public String getShardId() {
        return this.shardId;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public double getCpuUtilization() {
        return this.cpuUtilization;
    }

    public CriteriaEnum getCriteria() {
        return this.criteria;
    }

    @Override
    public HotShardSummaryMessage buildSummaryMessage() {
        final HotShardSummaryMessage.Builder summaryMessageBuilder =
                HotShardSummaryMessage.newBuilder();
        summaryMessageBuilder.setIndexName(this.indexName);
        summaryMessageBuilder.setShardId(this.shardId);
        summaryMessageBuilder.setNodeId(this.nodeId);
        summaryMessageBuilder.setCpuUtilization(this.cpuUtilization);
        summaryMessageBuilder.setCriteria(this.criteria);
        summaryMessageBuilder.setTimePeriod(this.timePeriodInSeconds);
        return summaryMessageBuilder.build();
    }

    @Override
    public void buildSummaryMessageAndAddToFlowUnit(FlowUnitMessage.Builder messageBuilder) {
        messageBuilder.setHotShardSummary(this.buildSummaryMessage());
    }

    public static HotShardSummary buildHotShardSummaryFromMessage(HotShardSummaryMessage message) {
        HotShardSummary summary =
                new HotShardSummary(
                        message.getIndexName(),
                        message.getShardId(),
                        message.getNodeId(),
                        message.getTimePeriod());
        summary.setCpuUtilization(message.getCpuUtilization());
        summary.setCriteria(message.getCriteria());
        return summary;
    }

    @Override
    public String toString() {
        return String.join(
                " ",
                new String[] {
                    this.indexName,
                    this.shardId,
                    this.nodeId,
                    String.valueOf(this.cpuUtilization),
                    String.valueOf(this.criteria)
                });
    }

    @Override
    public String getTableName() {
        return HotShardSummary.HOT_SHARD_SUMMARY_TABLE;
    }

    @Override
    public List<Field<?>> getSqlSchema() {
        List<Field<?>> schema = new ArrayList<>();
        schema.add(HotShardSummaryField.INDEX_NAME_FIELD.getField());
        schema.add(HotShardSummaryField.SHARD_ID_FIELD.getField());
        schema.add(HotShardSummaryField.NODE_ID_FIELD.getField());
        schema.add(HotShardSummaryField.CPU_UTILIZATION_FIELD.getField());
        schema.add(HotShardSummaryField.CRITERIA_FIELD.getField());
        schema.add(HotShardSummaryField.TIME_PERIOD_FIELD.getField());
        return schema;
    }

    @Override
    public List<Object> getSqlValue() {
        List<Object> value = new ArrayList<>();
        value.add(this.indexName);
        value.add(this.shardId);
        value.add(this.nodeId);
        value.add(this.cpuUtilization);
        value.add(this.criteria.getNumber());
        value.add(Integer.valueOf(this.timePeriodInSeconds));
        return value;
    }

    /**
     * Convert this summary object to JsonElement
     *
     * @return JsonElement
     */
    @Override
    public JsonElement toJson() {
        JsonObject summaryObj = new JsonObject();
        summaryObj.addProperty(SQL_SCHEMA_CONSTANTS.INDEX_NAME_COL_NAME, this.indexName);
        summaryObj.addProperty(SQL_SCHEMA_CONSTANTS.SHARD_ID_COL_NAME, this.shardId);
        summaryObj.addProperty(SQL_SCHEMA_CONSTANTS.NODE_ID_COL_NAME, this.nodeId);
        summaryObj.addProperty(SQL_SCHEMA_CONSTANTS.CPU_UTILIZATION_COL_NAME, this.cpuUtilization);
        summaryObj.addProperty(SQL_SCHEMA_CONSTANTS.CRITERIA_COL_NAME, this.criteria.toString());
        summaryObj.addProperty(SQL_SCHEMA_CONSTANTS.TIME_PERIOD_COL_NAME, this.timePeriodInSeconds);
        return summaryObj;
    }

    public static class SQL_SCHEMA_CONSTANTS {
        public static final String INDEX_NAME_COL_NAME = "index_name";
        public static final String SHARD_ID_COL_NAME = "shard_id";
        public static final String NODE_ID_COL_NAME = "node_id";
        public static final String CPU_UTILIZATION_COL_NAME = "cpu_utilization";
        public static final String CRITERIA_COL_NAME = "criteria";
        public static final String TIME_PERIOD_COL_NAME = "time_period";
    }

    /** Cluster summary SQL fields */
    public enum HotShardSummaryField implements JooqFieldValue {
        INDEX_NAME_FIELD(SQL_SCHEMA_CONSTANTS.INDEX_NAME_COL_NAME, String.class),
        SHARD_ID_FIELD(SQL_SCHEMA_CONSTANTS.SHARD_ID_COL_NAME, String.class),
        NODE_ID_FIELD(SQL_SCHEMA_CONSTANTS.NODE_ID_COL_NAME, String.class),
        CPU_UTILIZATION_FIELD(SQL_SCHEMA_CONSTANTS.CPU_UTILIZATION_COL_NAME, Double.class),
        CRITERIA_FIELD(SQL_SCHEMA_CONSTANTS.CRITERIA_COL_NAME, Integer.class),
        TIME_PERIOD_FIELD(SQL_SCHEMA_CONSTANTS.TIME_PERIOD_COL_NAME, Integer.class);

        private String name;
        private Class<?> clazz;

        HotShardSummaryField(final String name, Class<?> clazz) {
            this.name = name;
            this.clazz = clazz;
        }

        @Override
        public Field<?> getField() {
            return DSL.field(DSL.name(this.name), this.clazz);
        }

        @Override
        public String getName() {
            return this.name;
        }
    }

    /**
     * Re-generate the node summary object from SQL query result.
     *
     * @param record SQLite record
     * @return node summary object
     */
    @Nullable
    public static HotShardSummary buildSummary(final Record record) {
        if (record == null) {
            return null;
        }
        HotShardSummary summary = null;
        try {
            String indexName =
                    record.get(HotShardSummaryField.INDEX_NAME_FIELD.getField(), String.class);
            String shardId =
                    record.get(HotShardSummaryField.SHARD_ID_FIELD.getField(), String.class);
            String nodeId = record.get(HotShardSummaryField.NODE_ID_FIELD.getField(), String.class);
            Double cpuUtilization =
                    record.get(HotShardSummaryField.CPU_UTILIZATION_FIELD.getField(), Double.class);
            Integer criteria =
                    record.get(HotShardSummaryField.CRITERIA_FIELD.getField(), Integer.class);

            Integer timePeriod =
                    record.get(HotShardSummaryField.TIME_PERIOD_FIELD.getField(), Integer.class);
            if (timePeriod == null || cpuUtilization == null || criteria == null) {
                LOG.warn(
                        "read null object from SQL, timePeriod: {},  cpuUtilization: {},"
                                + " criteria: {}",
                        timePeriod,
                        cpuUtilization,
                        criteria);
                return null;
            }
            summary = new HotShardSummary(indexName, shardId, nodeId, timePeriod);
            summary.setCpuUtilization(cpuUtilization);
            summary.setCriteria(CriteriaEnum.forNumber(criteria));
        } catch (IllegalArgumentException ie) {
            LOG.error("Some fields might not be found in record, cause : {}", ie.getMessage());
        } catch (DataTypeException de) {
            LOG.error("Fails to convert data type");
        }
        return summary;
    }
}
