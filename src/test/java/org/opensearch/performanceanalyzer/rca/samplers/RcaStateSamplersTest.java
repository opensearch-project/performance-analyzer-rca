/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.samplers;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opensearch.performanceanalyzer.AppContext;

public class RcaStateSamplersTest {
    private RcaStateSamplers uut;

    @Test
    public void testGetRcaEnabledSampler() { // done for constructor coverage
        uut = new RcaStateSamplers();
        assertSame(uut.getClass(), RcaStateSamplers.class);
        assertTrue(
                RcaStateSamplers.getRcaEnabledSampler(new AppContext())
                        instanceof RcaEnabledSampler);
    }
}
