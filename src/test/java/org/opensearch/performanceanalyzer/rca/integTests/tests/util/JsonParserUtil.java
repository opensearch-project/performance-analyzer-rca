/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.tests.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class JsonParserUtil {
    public static int getSummaryJsonSize(JsonObject jsonObject, String summaryName) {
        JsonArray array = jsonObject.get(summaryName).getAsJsonArray();
        if (array == null) {
            return 0;
        }
        return array.size();
    }

    public static JsonObject getSummaryJson(JsonObject jsonObject, String summaryName, int idx) {
        JsonArray array = jsonObject.get(summaryName).getAsJsonArray();
        if (array == null) {
            return null;
        }
        if (idx >= array.size()) {
            return null;
        }
        return array.get(idx).getAsJsonObject();
    }
}
