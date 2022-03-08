/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer;


import org.junit.Assert;
import org.junit.Test;

public class PerformanceAnalyzerAppTest {

    @Test
    public void testMain() {
        PerformanceAnalyzerApp.main(new String[0]);
        Assert.assertFalse(ConfigStatus.INSTANCE.haveValidConfig());
    }
}
