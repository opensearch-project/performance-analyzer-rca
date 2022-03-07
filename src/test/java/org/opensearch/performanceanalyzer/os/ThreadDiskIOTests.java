/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.os;


import org.junit.Test;

public class ThreadDiskIOTests {
    public static void main(String[] args) throws Exception {
        runOnce();
    }

    public static void runOnce() {
        ThreadDiskIO.addSample();
        System.out.println(ThreadDiskIO.getIOUtilization().toString());
    }

    // - to enhance
    @Test
    public void testMetrics() {}
}
