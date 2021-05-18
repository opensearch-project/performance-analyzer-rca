/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.opensearch.performanceanalyzer.rca.integTests.tests.jvm.young_gen.validator;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.opensearch.performanceanalyzer.grpc.MetricEnum;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.integTests.framework.api.IValidator;

public class YoungGenRcaValidator implements IValidator {
    long startTime;

    public YoungGenRcaValidator() {
        startTime = System.currentTimeMillis();
    }

    /**
     * {"rca_name":"ClusterRca", "timestamp":1596557050522, "state":"unhealthy",
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
            if (object.get("rca_name").getAsString().equals("HighHeapUsageYoungGenRca")) {
                return checkClusterRca(object);
            }
        }
        return false;
    }

    /**
     * {"rca_name":"ClusterRca", "timestamp":1597167456322, "state":"unhealthy",
     * "HotClusterSummary":[{"number_of_nodes":1,"number_of_unhealthy_nodes":1}] }
     */
    boolean checkClusterRca(JsonObject object) {
        if (!"unhealthy".equals(object.get("state").getAsString())) {
            return false;
        }

        JsonElement elem = object.get("HotResourceSummary");
        if (elem == null) {
            return false;
        }
        System.out.println("ENTERING JSON PARSING");
        JsonArray array = elem.getAsJsonArray();

        Assert.assertEquals(1, array.size());
        Assert.assertEquals(
                ResourceEnum.OLD_GEN.toString(),
                array.get(0)
                        .getAsJsonObject()
                        .get(HotResourceSummary.SQL_SCHEMA_CONSTANTS.RESOURCE_TYPE_COL_NAME)
                        .getAsString());
        Assert.assertEquals(
                MetricEnum.FULL_GC.toString(),
                array.get(0)
                        .getAsJsonObject()
                        .get(HotResourceSummary.SQL_SCHEMA_CONSTANTS.RESOURCE_METRIC_COL_NAME)
                        .getAsString());
        System.out.println("MATCHED MY BABY");
        return true;
    }
}
