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
