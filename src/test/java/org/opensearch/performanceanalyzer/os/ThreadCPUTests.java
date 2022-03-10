/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.os;


import org.junit.Test;

public class ThreadCPUTests {
    public static void main(String[] args) throws Exception {
        runOnce();
    }

    private static void runOnce() {
        ThreadCPU.INSTANCE.addSample();
        System.out.println(
                "cpumap and pagemap:" + ThreadCPU.INSTANCE.getCPUPagingActivity().toString());
    }

    // - to enhance
    @Test
    public void testMetrics() {}
}
