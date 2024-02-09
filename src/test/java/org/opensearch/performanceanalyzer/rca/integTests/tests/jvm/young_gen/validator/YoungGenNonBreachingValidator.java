/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.tests.jvm.young_gen.validator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.Assert;
import org.opensearch.performanceanalyzer.decisionmaker.actions.HeapSizeIncreaseAction;
import org.opensearch.performanceanalyzer.decisionmaker.actions.JvmGenAction;
import org.opensearch.performanceanalyzer.rca.integTests.framework.api.IValidator;
import org.opensearch.performanceanalyzer.rca.persistence.actions.PersistedAction;
import org.opensearch.performanceanalyzer.rest.QueryActionRequestHandler;

public class YoungGenNonBreachingValidator implements IValidator {
    @Override
    public boolean checkJsonResp(JsonElement response) {
        JsonArray array =
                response.getAsJsonObject()
                        .get(QueryActionRequestHandler.ACTION_SET_JSON_NAME)
                        .getAsJsonArray();
        // It could well be the case that no RCA has been triggered so far, and thus no action
        // exists.
        // This is a valid outcome.
        if (array.size() == 0) {
            return true;
        }
        return checkPersistedAction(array);
    }

    private boolean checkPersistedAction(JsonArray array) {
        for (int i = 0; i < array.size(); i++) {
            JsonObject object = array.get(i).getAsJsonObject();
            // validate no heapSizeIncreaseAction is emitted
            Assert.assertNotEquals(
                    HeapSizeIncreaseAction.NAME,
                    object.get(PersistedAction.SQL_SCHEMA_CONSTANTS.ACTION_COL_NAME).getAsString());

            // validate no young gen action is emitted
            Assert.assertNotEquals(
                    JvmGenAction.NAME,
                    object.get(PersistedAction.SQL_SCHEMA_CONSTANTS.ACTION_COL_NAME).getAsString());
        }
        return true;
    }
}
