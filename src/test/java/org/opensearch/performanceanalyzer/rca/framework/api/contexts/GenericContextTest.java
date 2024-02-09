/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.contexts;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericContext;

public class GenericContextTest {
    private static class ConcreteGenericContext extends GenericContext {
        public ConcreteGenericContext(Resources.State state) {
            super(state);
        }
    }

    private ConcreteGenericContext uut;

    @Before
    public void setup() {
        uut = new ConcreteGenericContext(Resources.State.HEALTHY);
    }

    @Test
    public void testToString() {
        Assert.assertEquals(Resources.State.HEALTHY, uut.getState());
        Assert.assertEquals(Resources.State.HEALTHY.toString(), uut.toString());
    }

    @Test
    public void testIsUnknown() {
        Assert.assertFalse(uut.isUnknown());
        ConcreteGenericContext unknown = new ConcreteGenericContext(Resources.State.UNKNOWN);
        Assert.assertTrue(unknown.isUnknown());
    }

    @Test
    public void testIsUnhealthy() {
        Assert.assertFalse(ResourceContext.generic().isUnhealthy());
        Assert.assertFalse(uut.isUnhealthy());
        uut = new ConcreteGenericContext(Resources.State.CONTENDED);
        Assert.assertTrue(uut.isUnhealthy());
        uut = new ConcreteGenericContext(Resources.State.UNHEALTHY);
        Assert.assertTrue(uut.isUnhealthy());
    }
}
