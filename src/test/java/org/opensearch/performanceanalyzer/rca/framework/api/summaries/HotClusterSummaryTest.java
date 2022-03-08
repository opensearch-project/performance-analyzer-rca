/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.summaries;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.GeneratedMessageV3;
import java.util.List;
import org.jooq.Field;
import org.jooq.Record;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericSummary;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;

public class HotClusterSummaryTest {
    private static final int NUM_OF_NODES = 9;
    private static final int NUM_UNHEALTHY = 1;
    private static final String NODE_ID = "abc";
    private static final String NODE_ADDRESS = "127.0.0.1";
    private static HotClusterSummary uut;

    @Mock private Record testRecord;

    @BeforeClass
    public static void setup() {
        uut = new HotClusterSummary(NUM_OF_NODES, NUM_UNHEALTHY);
    }

    @Test
    public void testBuildSummaryMessage() {
        GeneratedMessageV3 msg = uut.buildSummaryMessage();
        Assert.assertNull(msg);
    }

    @Test
    public void testBuildSummaryMessageAndAddToFlowUnit() {
        // No assertions need to be made here, this function is a noop in the uut
        FlowUnitMessage.Builder msgBuilder = FlowUnitMessage.newBuilder();
        uut.buildSummaryMessageAndAddToFlowUnit(msgBuilder);
    }

    @Test
    public void testToString() {
        Assert.assertEquals(
                NUM_OF_NODES + " " + NUM_UNHEALTHY + " " + uut.getNestedSummaryList(),
                uut.toString());
    }

    @Test
    public void testGetTableName() {
        Assert.assertEquals(HotClusterSummary.HOT_CLUSTER_SUMMARY_TABLE, uut.getTableName());
    }

    @Test
    public void testGetSqlSchema() {
        List<Field<?>> schema = uut.getSqlSchema();
        Assert.assertEquals(2, schema.size());
        Assert.assertEquals(
                HotClusterSummary.ClusterSummaryField.NUM_OF_NODES_FIELD.getField(), schema.get(0));
        Assert.assertEquals(
                HotClusterSummary.ClusterSummaryField.NUM_OF_UNHEALTHY_NODES_FIELD.getField(),
                schema.get(1));
    }

    @Test
    public void testGetSqlValue() {
        List<Object> rows = uut.getSqlValue();
        Assert.assertEquals(2, rows.size());
        Assert.assertEquals(NUM_OF_NODES, rows.get(0));
        Assert.assertEquals(NUM_UNHEALTHY, rows.get(1));
    }

    @Test
    public void testToJson() {
        HotNodeSummary nodeSummary =
                new HotNodeSummary(
                        new InstanceDetails.Id(NODE_ID), new InstanceDetails.Ip(NODE_ADDRESS));
        uut.appendNestedSummary(nodeSummary);
        JsonElement elem = uut.toJson();
        Assert.assertTrue(elem.isJsonObject());
        JsonObject json = ((JsonObject) elem);
        Assert.assertEquals(
                NUM_OF_NODES,
                json.get(HotClusterSummary.SQL_SCHEMA_CONSTANTS.NUM_OF_NODES_COL_NAME).getAsInt());
        Assert.assertEquals(
                NUM_UNHEALTHY,
                json.get(HotClusterSummary.SQL_SCHEMA_CONSTANTS.NUM_OF_UNHEALTHY_NODES_COL_NAME)
                        .getAsInt());
        String tableName = uut.getHotNodeSummaryList().get(0).getTableName();
        JsonObject nodeJson = json.get(tableName).getAsJsonArray().get(0).getAsJsonObject();
        Assert.assertEquals(
                NODE_ID,
                nodeJson.get(HotNodeSummary.SQL_SCHEMA_CONSTANTS.NODE_ID_COL_NAME).getAsString());
        Assert.assertEquals(
                NODE_ADDRESS,
                nodeJson.get(HotNodeSummary.SQL_SCHEMA_CONSTANTS.HOST_IP_ADDRESS_COL_NAME)
                        .getAsString());
    }

    @Test
    public void testBuildSummary() {
        Assert.assertNull(HotClusterSummary.buildSummary(null));
        MockitoAnnotations.initMocks(this);
        Mockito.when(
                        testRecord.get(
                                HotClusterSummary.ClusterSummaryField.NUM_OF_NODES_FIELD.getField(),
                                Integer.class))
                .thenReturn(NUM_OF_NODES);
        Mockito.when(
                        testRecord.get(
                                HotClusterSummary.ClusterSummaryField.NUM_OF_UNHEALTHY_NODES_FIELD
                                        .getField(),
                                Integer.class))
                .thenReturn(NUM_UNHEALTHY);
        GenericSummary summary = HotClusterSummary.buildSummary(testRecord);
        Assert.assertNotNull(summary);
        Assert.assertTrue(summary instanceof HotClusterSummary);
        Assert.assertEquals(NUM_OF_NODES, ((HotClusterSummary) summary).getNumOfNodes());
        Assert.assertEquals(NUM_UNHEALTHY, ((HotClusterSummary) summary).getNumOfUnhealthyNodes());
    }
}
