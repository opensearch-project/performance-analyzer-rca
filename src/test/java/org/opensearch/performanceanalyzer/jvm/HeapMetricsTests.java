/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.jvm;


import java.lang.management.MemoryUsage;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.Test;

public class HeapMetricsTests {
    public static void main(String[] args) throws Exception {
        runOnce();
    }

    private static void runOnce() {
        for (Map.Entry<String, Supplier<MemoryUsage>> entry :
                HeapMetrics.getMemoryUsageSuppliers().entrySet()) {
            MemoryUsage memoryUsage = entry.getValue().get();
            System.out.println(entry.getKey() + "_committed:" + memoryUsage.getCommitted());
            System.out.println(entry.getKey() + "_init" + memoryUsage.getInit());
            System.out.println(entry.getKey() + "_max" + memoryUsage.getMax());
            System.out.println(entry.getKey() + "_used" + memoryUsage.getUsed());
        }
    }

    // - to enhance
    @Test
    public void testMetrics() {}
}
