/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.tests.jvmsizing.validator;


import org.junit.Assert;
import org.opensearch.performanceanalyzer.decisionmaker.actions.HeapSizeIncreaseAction;
import org.opensearch.performanceanalyzer.rca.persistence.actions.PersistedAction;

public class HeapSizeIncreaseNonBreachingValidator extends HeapSizeIncreaseValidator {

    @Override
    public boolean checkDbObj(Object object) {
        // It could well be the case that no RCA has been triggered so far, and thus no table
        // exists.
        // This is a valid outcome.
        if (object == null) {
            return true;
        }

        PersistedAction persistedAction = (PersistedAction) object;
        return checkPersistedAction(persistedAction);
    }

    @Override
    public boolean checkPersistedAction(PersistedAction persistedAction) {
        Assert.assertNotEquals(HeapSizeIncreaseAction.NAME, persistedAction.getActionName());

        return true;
    }
}
