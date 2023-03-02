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
import org.opensearch.performanceanalyzer.rca.framework.api.persist.JooqFieldValue;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericSummary;

/**
 * HotShardSummary contains information such as the index_name, shard_id, node_id, cpu_utilization,
 * cpu_utilization_threshold io_throughput, io_throughput_threshold, io_sys_callrate,
 * io_sys_callrate_threshold and time_period.
 *
 * <p>The hot shard summary is created by node level and cluster level RCAs running on data nodes
 * and elected cluster_manager node resp. This object is persisted in SQLite table Table name :
 * HotClusterSummary
 *
 * <p>schema : | ID(primary key) | index_name | shard_id | node_id | cpu_utilization |
 * cpu_utilization_threshold | io_throughput | io_throughput_threshold | io_sys_callrate |
 * io_sys_callrate_threshold| ID in FlowUnit(foreign key)
 */
public class HotShardSummary extends GenericSummary {

    public static final String HOT_SHARD_SUMMARY_TABLE = HotShardSummary.class.getSimpleName();
    private static final Logger LOG = LogManager.getLogger(HotShardSummary.class);
    private final String indexName;
    private final String shardId;
    private final String nodeId;
    private double cpu_utilization;
    private double cpu_utilization_threshold;
    private double heap_alloc_rate;
    private double heap_alloc_rate_threshold;

    private int timePeriodInSeconds;

    public HotShardSummary(String indexName, String shardId, String nodeId, int timePeriod) {
        super();
        this.indexName = indexName;
        this.shardId = shardId;
        this.nodeId = nodeId;
        this.timePeriodInSeconds = timePeriod;
    }

    public void setcpuUtilization(final double cpu_utilization) {
        this.cpu_utilization = cpu_utilization;
    }

    public void setCpuUtilizationThreshold(final double cpu_utilization_threshold) {
        this.cpu_utilization_threshold = cpu_utilization_threshold;
    }

    public void setHeapAllocRate(final double heap_alloc_rate) {
        this.heap_alloc_rate = heap_alloc_rate;
    }

    public void setHeapAllocRateThreshold(final double heap_alloc_rate_threshold) {
        this.heap_alloc_rate_threshold = heap_alloc_rate_threshold;
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
        return this.cpu_utilization;
    }

    public double getHeapAllocRate() {
        return this.heap_alloc_rate;
    }

    @Override
    public HotShardSummaryMessage buildSummaryMessage() {
        final HotShardSummaryMessage.Builder summaryMessageBuilder =
                HotShardSummaryMessage.newBuilder();
        summaryMessageBuilder.setIndexName(this.indexName);
        summaryMessageBuilder.setShardId(this.shardId);
        summaryMessageBuilder.setNodeId(this.nodeId);
        summaryMessageBuilder.setCpuUtilization(this.cpu_utilization);
        summaryMessageBuilder.setCpuUtilizationThreshold(this.cpu_utilization_threshold);
        summaryMessageBuilder.setHeapAllocRate(this.heap_alloc_rate);
        summaryMessageBuilder.setHeapAllocRateThreshold(this.heap_alloc_rate_threshold);
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
        summary.setcpuUtilization(message.getCpuUtilization());
        summary.setCpuUtilizationThreshold(message.getCpuUtilizationThreshold());
        summary.setHeapAllocRate(message.getHeapAllocRate());
        summary.setHeapAllocRateThreshold(message.getHeapAllocRateThreshold());
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
                    String.valueOf(this.cpu_utilization),
                    String.valueOf(this.cpu_utilization_threshold),
                    String.valueOf(this.heap_alloc_rate),
                    String.valueOf(this.heap_alloc_rate_threshold),
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
        schema.add(HotShardSummaryField.CPU_UTILIZATION_THRESHOLD_FIELD.getField());
        schema.add(HotShardSummaryField.HEAP_ALLOC_RATE_FIELD.getField());
        schema.add(HotShardSummaryField.HEAP_ALLOC_RATE_THRESHOLD_FIELD.getField());
        schema.add(HotShardSummaryField.TIME_PERIOD_FIELD.getField());
        return schema;
    }

    @Override
    public List<Object> getSqlValue() {
        List<Object> value = new ArrayList<>();
        value.add(this.indexName);
        value.add(this.shardId);
        value.add(this.nodeId);
        value.add(this.cpu_utilization);
        value.add(this.cpu_utilization_threshold);
        value.add(this.heap_alloc_rate);
        value.add(this.heap_alloc_rate_threshold);
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
        summaryObj.addProperty(SQL_SCHEMA_CONSTANTS.CPU_UTILIZATION_COL_NAME, this.cpu_utilization);
        summaryObj.addProperty(
                SQL_SCHEMA_CONSTANTS.CPU_UTILIZATION_THRESHOLD_COL_NAME,
                this.cpu_utilization_threshold);
        summaryObj.addProperty(SQL_SCHEMA_CONSTANTS.HEAP_ALLOC_RATE_COL_NAME, this.heap_alloc_rate);
        summaryObj.addProperty(
                SQL_SCHEMA_CONSTANTS.HEAP_ALLOC_RATE_THRESHOLD_COL_NAME,
                this.heap_alloc_rate_threshold);
        summaryObj.addProperty(SQL_SCHEMA_CONSTANTS.TIME_PERIOD_COL_NAME, this.timePeriodInSeconds);
        return summaryObj;
    }

    public static class SQL_SCHEMA_CONSTANTS {
        public static final String INDEX_NAME_COL_NAME = "index_name";
        public static final String SHARD_ID_COL_NAME = "shard_id";
        public static final String NODE_ID_COL_NAME = "node_id";
        public static final String CPU_UTILIZATION_COL_NAME = "cpu_utilization";
        public static final String CPU_UTILIZATION_THRESHOLD_COL_NAME = "cpu_utilization_threshold";
        public static final String HEAP_ALLOC_RATE_COL_NAME = "heap_alloc_rate";
        public static final String HEAP_ALLOC_RATE_THRESHOLD_COL_NAME = "heap_alloc_rate_threshold";
        public static final String TIME_PERIOD_COL_NAME = "time_period";
    }

    /** Cluster summary SQL fields */
    public enum HotShardSummaryField implements JooqFieldValue {
        INDEX_NAME_FIELD(SQL_SCHEMA_CONSTANTS.INDEX_NAME_COL_NAME, String.class),
        SHARD_ID_FIELD(SQL_SCHEMA_CONSTANTS.SHARD_ID_COL_NAME, String.class),
        NODE_ID_FIELD(SQL_SCHEMA_CONSTANTS.NODE_ID_COL_NAME, String.class),
        CPU_UTILIZATION_FIELD(SQL_SCHEMA_CONSTANTS.CPU_UTILIZATION_COL_NAME, Double.class),
        CPU_UTILIZATION_THRESHOLD_FIELD(
                SQL_SCHEMA_CONSTANTS.CPU_UTILIZATION_THRESHOLD_COL_NAME, Double.class),
        HEAP_ALLOC_RATE_FIELD(SQL_SCHEMA_CONSTANTS.HEAP_ALLOC_RATE_COL_NAME, Double.class),
        HEAP_ALLOC_RATE_THRESHOLD_FIELD(
                SQL_SCHEMA_CONSTANTS.HEAP_ALLOC_RATE_THRESHOLD_COL_NAME, Double.class),
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
            Double cpu_utilization =
                    record.get(HotShardSummaryField.CPU_UTILIZATION_FIELD.getField(), Double.class);
            Double cpu_utilization_threshold =
                    record.get(
                            HotShardSummaryField.CPU_UTILIZATION_THRESHOLD_FIELD.getField(),
                            Double.class);
            Double heap_alloc_rate =
                    record.get(HotShardSummaryField.HEAP_ALLOC_RATE_FIELD.getField(), Double.class);
            Double heap_alloc_rate_threshold =
                    record.get(
                            HotShardSummaryField.HEAP_ALLOC_RATE_THRESHOLD_FIELD.getField(),
                            Double.class);

            Integer timePeriod =
                    record.get(HotShardSummaryField.TIME_PERIOD_FIELD.getField(), Integer.class);
            if (timePeriod == null
                    || cpu_utilization == null
                    || cpu_utilization_threshold == null
                    || heap_alloc_rate == null
                    || heap_alloc_rate_threshold == null) {
                LOG.warn(
                        "read null object from SQL, timePeriod: {},  cpu_utilization: {}, cpu_utilization_threshold: {},"
                                + " heap_alloc_rate: {},  heap_alloc_rate_threshold: {}",
                        timePeriod,
                        cpu_utilization,
                        cpu_utilization_threshold,
                        heap_alloc_rate,
                        heap_alloc_rate_threshold);
                return null;
            }
            summary = new HotShardSummary(indexName, shardId, nodeId, timePeriod);
            summary.setcpuUtilization(cpu_utilization);
            summary.setCpuUtilizationThreshold(cpu_utilization_threshold);
            summary.setHeapAllocRate(heap_alloc_rate);
            summary.setHeapAllocRateThreshold(heap_alloc_rate_threshold);
        } catch (IllegalArgumentException ie) {
            LOG.error("Some fields might not be found in record, cause : {}", ie.getMessage());
        } catch (DataTypeException de) {
            LOG.error("Fails to convert data type");
        }
        return summary;
    }
}
