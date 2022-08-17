/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.tests.jvmsizing.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.opensearch.performanceanalyzer.decisionmaker.actions.HeapSizeIncreaseAction;
import org.opensearch.performanceanalyzer.rca.persistence.actions.PersistedAction;

public class HeapSizeIncreaseValidatorDedicatedClusterManager extends HeapSizeIncreaseValidator {

    @Override
    public boolean checkPersistedAction(final PersistedAction persistedAction) {
        assertTrue(persistedAction.isActionable());
        assertFalse(persistedAction.isMuted());
        Assert.assertEquals(HeapSizeIncreaseAction.NAME, persistedAction.getActionName());
        assertEquals(TimeUnit.DAYS.toMillis(3), persistedAction.getCoolOffPeriod());
        assertEquals("{DATA_0,DATA_1}", persistedAction.getNodeIds());
        assertEquals("{127.0.0.1,127.0.0.1}", persistedAction.getNodeIps());
        return true;
    }
}
