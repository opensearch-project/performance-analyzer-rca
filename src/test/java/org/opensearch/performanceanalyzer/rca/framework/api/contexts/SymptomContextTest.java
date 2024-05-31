/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.contexts;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;

public class SymptomContextTest {
    private SymptomContext uut;

    @Before
    public void setup() {
        uut = new SymptomContext(Resources.State.HEALTHY);
    }

    @Test
    public void testGeneric() {
        SymptomContext generic = SymptomContext.generic();
        Assert.assertSame(Resources.State.UNKNOWN, generic.getState());
    }
}
