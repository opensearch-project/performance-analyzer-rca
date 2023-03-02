/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.collectors;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metrics.PerformanceAnalyzerMetrics;

public class HeapMetricsCollectorGCTypesTest {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HeapMetricsCollector uut = new HeapMetricsCollector();
    private static final Set<String> gcTypes =
            ImmutableSet.of(
                    AllMetrics.GCType.HEAP.toString(),
                    AllMetrics.GCType.NON_HEAP.toString(),
                    AllMetrics.GCType.PERM_GEN.toString(),
                    AllMetrics.GCType.OLD_GEN.toString(),
                    AllMetrics.GCType.TOT_YOUNG_GC.toString(),
                    AllMetrics.GCType.TOT_FULL_GC.toString(),
                    AllMetrics.GCType.EDEN.toString(),
                    AllMetrics.GCType.SURVIVOR.toString());

    @Test
    public void validateCollectedGCTypes() throws Exception {
        Set<String> collectedGCTypes = new HashSet<>();

        uut.collectMetrics(Instant.now().toEpochMilli());
        String metricString = uut.getValue().toString();
        int end = metricString.indexOf(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);
        metricString = metricString.substring(end + 1);

        while (!metricString.isEmpty()) {
            end = metricString.indexOf(PerformanceAnalyzerMetrics.sMetricNewLineDelimitor);
            String metric = metricString.substring(0, end);
            HeapMetricsCollector.HeapStatus heapStatus =
                    mapper.readValue(metric, HeapMetricsCollector.HeapStatus.class);
            collectedGCTypes.add(heapStatus.getType());
            metricString = metricString.substring(end + 1);
        }

        Assert.assertEquals(collectedGCTypes, gcTypes);
    }
}
