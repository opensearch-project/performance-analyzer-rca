/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.exceptions;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MalformedAnalysisGraphTest {
    private static final String MSG = "MSG";

    private MalformedAnalysisGraph uut;

    @Before
    public void setup() {
        uut = new MalformedAnalysisGraph(MSG);
    }

    @Test
    public void testConstruction() {
        Assert.assertEquals(MSG, uut.getMessage());
    }
}
