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

import static org.junit.Assert.assertTrue;

import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.decisionmaker.actions.JvmGenAction;
import org.opensearch.performanceanalyzer.rca.integTests.framework.api.IValidator;
import org.opensearch.performanceanalyzer.rca.persistence.actions.PersistedAction;

public class JvmGenActionValidator implements IValidator {
    AppContext appContext;
    long startTime;

    public JvmGenActionValidator() {
        appContext = new AppContext();
        startTime = System.currentTimeMillis();
    }

    @Override
    public boolean checkDbObj(Object object) {
        if (object == null) {
            return false;
        }

        PersistedAction persistedAction = (PersistedAction) object;
        return checkPersistedAction(persistedAction);
    }

    private boolean checkPersistedAction(final PersistedAction persistedAction) {
        JvmGenAction heapSizeIncreaseAction =
                JvmGenAction.fromSummary(persistedAction.getSummary(), appContext);
        assertTrue(heapSizeIncreaseAction.getTargetRatio() <= 5);
        return true;
    }
}
