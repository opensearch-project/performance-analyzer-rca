/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.os;


import org.junit.Test;

public class ThreadSchedTests {
    public static void main(String[] args) throws Exception {
        runOnce();
    }

    public static void runOnce() {
        ThreadSched.INSTANCE.addSample();
        System.out.println(ThreadSched.INSTANCE.getSchedLatency().toString());
    }

    // - to enhance
    @Test
    public void testMetrics() {}
}
