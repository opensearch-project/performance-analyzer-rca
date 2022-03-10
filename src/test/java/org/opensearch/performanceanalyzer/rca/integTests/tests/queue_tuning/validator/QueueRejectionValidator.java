/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.tests.queue_tuning.validator;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.integTests.framework.api.IValidator;
import org.opensearch.performanceanalyzer.rca.integTests.tests.util.JsonParserUtil;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.QueueRejectionClusterRca;

public class QueueRejectionValidator implements IValidator {
    long startTime;

    public QueueRejectionValidator() {
        startTime = System.currentTimeMillis();
    }

    /**
     * {"rca_name":"QueueRejectionClusterRca", "timestamp":1596557050522, "state":"unhealthy",
     * "HotClusterSummary":[ {"number_of_nodes":1,"number_of_unhealthy_nodes":1} ]}
     */
    @Override
    public boolean checkJsonResp(JsonElement response) {
        JsonArray array = response.getAsJsonObject().get("data").getAsJsonArray();
        if (array.size() == 0) {
            return false;
        }

        for (int i = 0; i < array.size(); i++) {
            JsonObject object = array.get(i).getAsJsonObject();
            if (object.get("rca_name")
                    .getAsString()
                    .equals(QueueRejectionClusterRca.RCA_TABLE_NAME)) {
                return checkClusterRca(object);
            }
        }
        return false;
    }

    /**
     * {"rca_name":"QueueRejectionClusterRca", "timestamp":1597167456322, "state":"unhealthy",
     * "HotClusterSummary":[{"number_of_nodes":1,"number_of_unhealthy_nodes":1}] }
     */
    boolean checkClusterRca(JsonObject rcaObject) {
        if (!"unhealthy".equals(rcaObject.get("state").getAsString())) {
            return false;
        }
        Assert.assertEquals(
                1,
                JsonParserUtil.getSummaryJsonSize(
                        rcaObject, HotClusterSummary.HOT_CLUSTER_SUMMARY_TABLE));
        JsonObject clusterSummaryJson =
                JsonParserUtil.getSummaryJson(
                        rcaObject, HotClusterSummary.HOT_CLUSTER_SUMMARY_TABLE, 0);
        Assert.assertNotNull(clusterSummaryJson);
        Assert.assertEquals(1, clusterSummaryJson.get("number_of_unhealthy_nodes").getAsInt());

        Assert.assertEquals(
                1,
                JsonParserUtil.getSummaryJsonSize(
                        clusterSummaryJson, HotNodeSummary.HOT_NODE_SUMMARY_TABLE));
        JsonObject nodeSummaryJson =
                JsonParserUtil.getSummaryJson(
                        clusterSummaryJson, HotNodeSummary.HOT_NODE_SUMMARY_TABLE, 0);
        Assert.assertNotNull(nodeSummaryJson);
        Assert.assertEquals(
                "DATA_0",
                nodeSummaryJson
                        .get(HotNodeSummary.SQL_SCHEMA_CONSTANTS.NODE_ID_COL_NAME)
                        .getAsString());
        Assert.assertEquals(
                "127.0.0.1",
                nodeSummaryJson
                        .get(HotNodeSummary.SQL_SCHEMA_CONSTANTS.HOST_IP_ADDRESS_COL_NAME)
                        .getAsString());

        Assert.assertEquals(
                1,
                JsonParserUtil.getSummaryJsonSize(
                        nodeSummaryJson, HotResourceSummary.HOT_RESOURCE_SUMMARY_TABLE));
        JsonObject resourceSummaryJson =
                JsonParserUtil.getSummaryJson(
                        nodeSummaryJson, HotResourceSummary.HOT_RESOURCE_SUMMARY_TABLE, 0);
        Assert.assertNotNull(resourceSummaryJson);
        Assert.assertEquals(
                "write threadpool",
                resourceSummaryJson
                        .get(HotResourceSummary.SQL_SCHEMA_CONSTANTS.RESOURCE_TYPE_COL_NAME)
                        .getAsString());
        return true;
    }
}
