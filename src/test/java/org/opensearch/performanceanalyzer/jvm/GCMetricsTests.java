/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.jvm;


import org.junit.Test;

public class GCMetricsTests {
    public static void main(String[] args) throws Exception {
        runOnce();
    }

    private static void runOnce() {
        GCMetrics.runGCMetrics();
        GCMetrics.printGCMetrics();
    }

    // - to enhance
    @Test
    public void testMetrics() {}
}
