/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
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
