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
 * Copyright 2020-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package org.opensearch.performanceanalyzer.rca.integTests.tests.collator.validator;

import static org.junit.Assert.assertEquals;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.decisionmaker.actions.HeapSizeIncreaseAction;
import org.opensearch.performanceanalyzer.rca.integTests.framework.api.IValidator;
import org.opensearch.performanceanalyzer.rca.persistence.actions.PersistedAction;
import org.opensearch.performanceanalyzer.rest.QueryActionRequestHandler;

public class CollatorValidator implements IValidator {

    private static final Logger LOG = LogManager.getLogger(CollatorValidator.class);
    protected AppContext appContext;
    protected long startTime;

    public CollatorValidator() {
        this.appContext = new AppContext();
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean checkJsonResp(JsonElement response) {
        JsonArray array =
                response.getAsJsonObject()
                        .get(QueryActionRequestHandler.ACTION_SET_JSON_NAME)
                        .getAsJsonArray();
        if (array.size() == 0) {
            return false;
        }

        assertEquals(1, array.size());
        JsonObject obj = array.get(0).getAsJsonObject();

        if (!obj.get(PersistedAction.SQL_SCHEMA_CONSTANTS.ACTION_COL_NAME)
                .getAsString()
                .equals(HeapSizeIncreaseAction.NAME)) {
            return false;
        }

        return obj.get(PersistedAction.SQL_SCHEMA_CONSTANTS.ACTIONABLE_NAME).getAsBoolean();
    }
}
