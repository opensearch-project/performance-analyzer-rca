/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.tests.jvm.old_gen_policy.validator;

import com.google.gson.JsonArray;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;

public class LevelThreeValidator extends OldGenPolicyBaseValidator {

    @Override
    public boolean checkPersistedActions(JsonArray actionJsonArray) {
        return (checkCacheClearAction(actionJsonArray)
                && checkModifyQueueAction(actionJsonArray, ResourceEnum.WRITE_THREADPOOL)
                && checkModifyQueueAction(actionJsonArray, ResourceEnum.SEARCH_THREADPOOL)
                && checkModifyCacheAction(actionJsonArray, ResourceEnum.FIELD_DATA_CACHE)
                && checkModifyCacheAction(actionJsonArray, ResourceEnum.SHARD_REQUEST_CACHE));
    }
}
