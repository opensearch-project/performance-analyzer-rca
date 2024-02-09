/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.admissioncontrol.heap;

import java.util.HashMap;
import java.util.Map;

/** HeapRcaFactory returns HeapRca based on maxHeap in gigabytes */
public class AdmissionControlByHeapFactory {

    // Heap Size in Gigabytes
    private static final int SMALL_HEAP_RCA_THRESHOLD = 4;
    private static final int MEDIUM_HEAP_RCA_THRESHOLD = 32;

    // Keys for rcaMap
    private static final String SMALL_HEAP = "SMALL_HEAP";
    private static final String MEDIUM_HEAP = "MEDIUM_HEAP";
    private static final String LARGE_HEAP = "LARGE_HEAP";

    private static final Map<String, AdmissionControlByHeap> rcaMap = new HashMap<>();

    public static AdmissionControlByHeap getByMaxHeap(double maxHeap) {
        if (maxHeap <= SMALL_HEAP_RCA_THRESHOLD) {
            if (!rcaMap.containsKey(SMALL_HEAP))
                rcaMap.put(SMALL_HEAP, new AdmissionControlBySmallHeap());
            return rcaMap.get(SMALL_HEAP);
        } else if (maxHeap <= MEDIUM_HEAP_RCA_THRESHOLD) {
            if (!rcaMap.containsKey(MEDIUM_HEAP))
                rcaMap.put(MEDIUM_HEAP, new AdmissionControlByMediumHeap());
            return rcaMap.get(MEDIUM_HEAP);
        } else {
            if (!rcaMap.containsKey(LARGE_HEAP))
                rcaMap.put(LARGE_HEAP, new AdmissionControlByLargeHeap());
            return rcaMap.get(LARGE_HEAP);
        }
    }
}
