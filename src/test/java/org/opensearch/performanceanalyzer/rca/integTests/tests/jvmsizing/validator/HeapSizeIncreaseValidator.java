/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.tests.jvmsizing.validator;

import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.rca.integTests.framework.api.IValidator;
import org.opensearch.performanceanalyzer.rca.persistence.actions.PersistedAction;

public abstract class HeapSizeIncreaseValidator implements IValidator {

    AppContext appContext;
    long startTime;

    public HeapSizeIncreaseValidator() {
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

    public abstract boolean checkPersistedAction(final PersistedAction persistedAction);
}
