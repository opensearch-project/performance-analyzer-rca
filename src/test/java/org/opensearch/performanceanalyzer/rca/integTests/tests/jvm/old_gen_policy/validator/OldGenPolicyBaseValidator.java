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

package org.opensearch.performanceanalyzer.rca.integTests.tests.jvm.old_gen_policy.validator;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.decisionmaker.actions.CacheClearAction;
import org.opensearch.performanceanalyzer.decisionmaker.actions.ModifyCacheMaxSizeAction;
import org.opensearch.performanceanalyzer.decisionmaker.actions.ModifyQueueCapacityAction;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;
import org.opensearch.performanceanalyzer.rca.integTests.framework.api.IValidator;
import org.opensearch.performanceanalyzer.rca.persistence.actions.PersistedAction;
import org.opensearch.performanceanalyzer.rest.QueryActionRequestHandler;

public abstract class OldGenPolicyBaseValidator implements IValidator {
    private static final Logger LOG = LogManager.getLogger(OldGenPolicyBaseValidator.class);
    private Gson gson;

    public OldGenPolicyBaseValidator() {
        gson = new Gson();
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

        return checkPersistedActions(array);
    }

    abstract boolean checkPersistedActions(JsonArray actionJsonArray);

    protected boolean checkModifyQueueAction(JsonArray array, ResourceEnum threadpool) {
        for (int i = 0; i < array.size(); i++) {
            JsonObject object = array.get(i).getAsJsonObject();
            if (!object.get(PersistedAction.SQL_SCHEMA_CONSTANTS.ACTION_COL_NAME)
                    .getAsString()
                    .equals(ModifyQueueCapacityAction.NAME)) {
                continue;
            }
            if (!object.get(PersistedAction.SQL_SCHEMA_CONSTANTS.ACTIONABLE_NAME).getAsBoolean()) {
                continue;
            }
            JsonObject summaryObj =
                    object.getAsJsonObject(PersistedAction.SQL_SCHEMA_CONSTANTS.SUMMARY_NAME);
            if (summaryObj != null) {
                try {
                    ModifyQueueCapacityAction.Summary summary =
                            gson.fromJson(summaryObj, ModifyQueueCapacityAction.Summary.class);
                    if (summary.getResource() == threadpool) {
                        return true;
                    }
                } catch (Exception e) {
                    LOG.warn(
                            "Json syntax error, parsing summary object : {}",
                            summaryObj.toString());
                }
            }
        }
        return false;
    }

    protected boolean checkModifyCacheAction(JsonArray array, ResourceEnum cacheType) {
        for (int i = 0; i < array.size(); i++) {
            JsonObject object = array.get(i).getAsJsonObject();
            if (!object.get(PersistedAction.SQL_SCHEMA_CONSTANTS.ACTION_COL_NAME)
                    .getAsString()
                    .equals(ModifyCacheMaxSizeAction.NAME)) {
                continue;
            }
            if (!object.get(PersistedAction.SQL_SCHEMA_CONSTANTS.ACTIONABLE_NAME).getAsBoolean()) {
                continue;
            }
            JsonObject summaryObj =
                    object.getAsJsonObject(PersistedAction.SQL_SCHEMA_CONSTANTS.SUMMARY_NAME);
            if (summaryObj != null) {
                try {
                    ModifyCacheMaxSizeAction.Summary summary =
                            gson.fromJson(summaryObj, ModifyCacheMaxSizeAction.Summary.class);
                    if (summary.getResource() == cacheType) {
                        return true;
                    }
                } catch (Exception e) {
                    LOG.warn(
                            "Json syntax error, parsing summary object : {}",
                            summaryObj.toString());
                }
            }
        }
        return false;
    }

    protected boolean checkCacheClearAction(JsonArray array) {
        for (int i = 0; i < array.size(); i++) {
            JsonObject object = array.get(i).getAsJsonObject();
            if (!object.get(PersistedAction.SQL_SCHEMA_CONSTANTS.ACTION_COL_NAME)
                    .getAsString()
                    .equals(CacheClearAction.NAME)) {
                continue;
            }
            if (!object.get(PersistedAction.SQL_SCHEMA_CONSTANTS.ACTIONABLE_NAME).getAsBoolean()) {
                continue;
            }
            return true;
        }
        return false;
    }
}
