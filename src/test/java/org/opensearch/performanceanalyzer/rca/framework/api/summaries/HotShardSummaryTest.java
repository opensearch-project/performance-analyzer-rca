/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.summaries;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import org.jooq.Field;
import org.jooq.Record;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.grpc.HotShardSummaryMessage;
import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericSummary;

public class HotShardSummaryTest {
    private final String INDEX_NAME = "index_1";
    private final String SHARD_ID = "shard_1";
    private final String NODE_ID = "node_1";
    private final double CPU_UTILIZATION = 0.65;
    private final double CPU_UTILIZATION_THRESHOLD = 0.10;
    private final Resource CPU_UTILIZATION_RESOURCE = ResourceUtil.CPU_USAGE;
    private final int TIME_PERIOD = 2020;

    private HotShardSummary uut;

    @Before
    public void setup() {
        uut = new HotShardSummary(INDEX_NAME, SHARD_ID, NODE_ID, TIME_PERIOD);
        uut.setResourceValue(CPU_UTILIZATION);
        uut.setResourceThreshold(CPU_UTILIZATION_THRESHOLD);
        uut.setResource(CPU_UTILIZATION_RESOURCE);
    }

    @Test
    public void testBuildSummaryMessage() {
        HotShardSummaryMessage msg = uut.buildSummaryMessage();
        Assert.assertEquals(NODE_ID, msg.getNodeId());
        Assert.assertEquals(INDEX_NAME, msg.getIndexName());
        Assert.assertEquals(SHARD_ID, msg.getShardId());
        Assert.assertEquals(CPU_UTILIZATION, msg.getResourceValue(), 0);
        Assert.assertEquals(CPU_UTILIZATION_THRESHOLD, msg.getResourceThreshold(), 0);
        Assert.assertEquals(CPU_UTILIZATION_RESOURCE, msg.getResource());
        Assert.assertEquals(TIME_PERIOD, msg.getTimePeriod());
    }

    @Test
    public void testBuildSummaryMessageAndAddToFlowUnit() {
        // No assertions need to be made here, this function is a noop in the uut
        FlowUnitMessage.Builder msgBuilder = FlowUnitMessage.newBuilder();
        uut.buildSummaryMessageAndAddToFlowUnit(msgBuilder);
        Assert.assertEquals(uut.buildSummaryMessage(), msgBuilder.getHotShardSummary());
    }

    @Test
    public void testToString() {
        String expected =
                String.join(
                        " ",
                        new String[] {
                            INDEX_NAME,
                            SHARD_ID,
                            NODE_ID,
                            String.valueOf(CPU_UTILIZATION),
                            String.valueOf(CPU_UTILIZATION_THRESHOLD),
                            String.valueOf(CPU_UTILIZATION_RESOURCE)
                        });
        Assert.assertEquals(expected, uut.toString());
    }

    @Test
    public void testGetTableName() {
        Assert.assertEquals(HotShardSummary.HOT_SHARD_SUMMARY_TABLE, uut.getTableName());
    }

    @Test
    public void testGetSqlSchema() {
        List<Field<?>> schema = uut.getSqlSchema();
        Assert.assertEquals(8, schema.size());
        Assert.assertEquals(
                HotShardSummary.HotShardSummaryField.INDEX_NAME_FIELD.getField(), schema.get(0));
        Assert.assertEquals(
                HotShardSummary.HotShardSummaryField.SHARD_ID_FIELD.getField(), schema.get(1));
        Assert.assertEquals(
                HotShardSummary.HotShardSummaryField.NODE_ID_FIELD.getField(), schema.get(2));
        Assert.assertEquals(
                HotShardSummary.HotShardSummaryField.RESOURCE_VALUE_FIELD.getField(),
                schema.get(3));
        Assert.assertEquals(
                HotShardSummary.HotShardSummaryField.RESOURCE_THRESHOLD_FIELD.getField(),
                schema.get(4));
        Assert.assertEquals(
                HotShardSummary.HotShardSummaryField.RESOURCE_TYPE_FIELD.getField(), schema.get(5));
        Assert.assertEquals(
                HotShardSummary.HotShardSummaryField.RESOURCE_METRIC_FIELD.getField(),
                schema.get(6));
        Assert.assertEquals(
                HotShardSummary.HotShardSummaryField.TIME_PERIOD_FIELD.getField(), schema.get(7));
    }

    @Test
    public void testGetSqlValue() {
        List<Object> values = uut.getSqlValue();
        Assert.assertEquals(8, values.size());
        Assert.assertEquals(INDEX_NAME, values.get(0));
        Assert.assertEquals(SHARD_ID, values.get(1));
        Assert.assertEquals(NODE_ID, values.get(2));
        Assert.assertEquals(CPU_UTILIZATION, values.get(3));
        Assert.assertEquals(CPU_UTILIZATION_THRESHOLD, values.get(4));
        Assert.assertEquals(CPU_UTILIZATION_RESOURCE.getResourceEnumValue(), values.get(5));
        Assert.assertEquals(CPU_UTILIZATION_RESOURCE.getMetricEnumValue(), values.get(6));
        Assert.assertEquals(TIME_PERIOD, values.get(7));
    }

    @Test
    public void testToJson() {
        JsonElement elem = uut.toJson();
        Assert.assertTrue(elem.isJsonObject());
        JsonObject json = ((JsonObject) elem);
        Assert.assertEquals(
                INDEX_NAME,
                json.get(HotShardSummary.SQL_SCHEMA_CONSTANTS.INDEX_NAME_COL_NAME).getAsString());
        Assert.assertEquals(
                SHARD_ID,
                json.get(HotShardSummary.SQL_SCHEMA_CONSTANTS.SHARD_ID_COL_NAME).getAsString());
        Assert.assertEquals(
                NODE_ID,
                json.get(HotShardSummary.SQL_SCHEMA_CONSTANTS.NODE_ID_COL_NAME).getAsString());
        Assert.assertEquals(
                CPU_UTILIZATION,
                json.get(HotShardSummary.SQL_SCHEMA_CONSTANTS.RESOURCE_VALUE_COL_NAME)
                        .getAsDouble(),
                0);
        Assert.assertEquals(
                CPU_UTILIZATION_THRESHOLD,
                json.get(HotShardSummary.SQL_SCHEMA_CONSTANTS.RESOURCE_THRESHOLD_COL_NAME)
                        .getAsDouble(),
                0);
        Assert.assertEquals(
                ResourceUtil.getResourceTypeName(CPU_UTILIZATION_RESOURCE),
                json.get(HotShardSummary.SQL_SCHEMA_CONSTANTS.RESOURCE_TYPE_COL_NAME)
                        .getAsString());
        Assert.assertEquals(
                ResourceUtil.getResourceMetricName(CPU_UTILIZATION_RESOURCE),
                json.get(HotShardSummary.SQL_SCHEMA_CONSTANTS.RESOURCE_METRIC_COL_NAME)
                        .getAsString());
        Assert.assertEquals(
                TIME_PERIOD,
                json.get(HotShardSummary.SQL_SCHEMA_CONSTANTS.TIME_PERIOD_COL_NAME).getAsDouble(),
                0);
    }

    @Test
    public void testBuildSummary() {
        Assert.assertNull(HotShardSummary.buildSummary(null));
        Record testRecord = Mockito.mock(Record.class);
        Mockito.when(
                        testRecord.get(
                                HotShardSummary.HotShardSummaryField.INDEX_NAME_FIELD.getField(),
                                String.class))
                .thenReturn(INDEX_NAME);
        Mockito.when(
                        testRecord.get(
                                HotShardSummary.HotShardSummaryField.SHARD_ID_FIELD.getField(),
                                String.class))
                .thenReturn(SHARD_ID);
        Mockito.when(
                        testRecord.get(
                                HotShardSummary.HotShardSummaryField.NODE_ID_FIELD.getField(),
                                String.class))
                .thenReturn(NODE_ID);
        Mockito.when(
                        testRecord.get(
                                HotShardSummary.HotShardSummaryField.RESOURCE_VALUE_FIELD
                                        .getField(),
                                Double.class))
                .thenReturn(CPU_UTILIZATION);
        Mockito.when(
                        testRecord.get(
                                HotShardSummary.HotShardSummaryField.RESOURCE_THRESHOLD_FIELD
                                        .getField(),
                                Double.class))
                .thenReturn(CPU_UTILIZATION_THRESHOLD);
        Mockito.when(
                        testRecord.get(
                                HotShardSummary.HotShardSummaryField.RESOURCE_TYPE_FIELD.getField(),
                                Integer.class))
                .thenReturn(CPU_UTILIZATION_RESOURCE.getResourceEnumValue());
        Mockito.when(
                        testRecord.get(
                                HotShardSummary.HotShardSummaryField.RESOURCE_METRIC_FIELD
                                        .getField(),
                                Integer.class))
                .thenReturn(CPU_UTILIZATION_RESOURCE.getMetricEnumValue());
        Mockito.when(
                        testRecord.get(
                                HotShardSummary.HotShardSummaryField.TIME_PERIOD_FIELD.getField(),
                                Integer.class))
                .thenReturn(TIME_PERIOD);
        GenericSummary summary = HotShardSummary.buildSummary(testRecord);
        Assert.assertNotNull(summary);
        List<Object> values = summary.getSqlValue();
        Assert.assertEquals(8, values.size());
        Assert.assertEquals(INDEX_NAME, values.get(0));
        Assert.assertEquals(SHARD_ID, values.get(1));
        Assert.assertEquals(NODE_ID, values.get(2));
        Assert.assertEquals(CPU_UTILIZATION, values.get(3));
        Assert.assertEquals(CPU_UTILIZATION_THRESHOLD, values.get(4));
        Assert.assertEquals(CPU_UTILIZATION_RESOURCE.getResourceEnumValue(), values.get(5));
        Assert.assertEquals(CPU_UTILIZATION_RESOURCE.getMetricEnumValue(), values.get(6));
        Assert.assertEquals(TIME_PERIOD, values.get(7));
    }
}
