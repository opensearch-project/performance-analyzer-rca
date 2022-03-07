/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.jvm;


import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class HeapMetrics {
    private static final Map<String, Supplier<MemoryUsage>> memoryUsageSuppliers;

    static {
        memoryUsageSuppliers = new HashMap<>();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        if (memoryMXBean != null) {
            memoryUsageSuppliers.put("Heap", () -> memoryMXBean.getHeapMemoryUsage());
            memoryUsageSuppliers.put("NonHeap", () -> memoryMXBean.getNonHeapMemoryUsage());
        }

        List<MemoryPoolMXBean> list = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean item : list) {
            if ("CMS Perm Gen".equals(item.getName())
                    || "Perm Gen".equals(item.getName())
                    || "PS Perm Gen".equals(item.getName())
                    || "G1 Perm Gen".equals(item.getName())
                    || "Metaspace".equals(item.getName())) {
                memoryUsageSuppliers.put("PermGen", () -> item.getUsage());
            } else if ("CMS Old Gen".equals(item.getName())
                    || "Tenured Gen".equals(item.getName())
                    || "PS Old Gen".equals(item.getName())
                    || "G1 Old Gen".equals(item.getName())) {
                memoryUsageSuppliers.put("OldGen", () -> item.getUsage());
            } else if ("Par Eden Space".equals(item.getName())
                    || "Eden Space".equals(item.getName())
                    || "PS Eden Space".equals(item.getName())
                    || "G1 Eden".equals(item.getName())) {
                memoryUsageSuppliers.put("Eden", () -> item.getUsage());
            } else if ("Par Survivor Space".equals(item.getName())
                    || "Survivor Space".equals(item.getName())
                    || "PS Survivor Space".equals(item.getName())
                    || "G1 Survivor".equals(item.getName())) {
                memoryUsageSuppliers.put("Survivor", () -> item.getUsage());
            }
        }
    }

    public static Map<String, Supplier<MemoryUsage>> getMemoryUsageSuppliers() {
        return memoryUsageSuppliers;
    }
}
