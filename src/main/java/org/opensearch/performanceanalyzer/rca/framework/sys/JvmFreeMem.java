/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.sys;


import org.opensearch.performanceanalyzer.rca.framework.metrics.JvmMetrics;
import org.opensearch.performanceanalyzer.rca.stats.collectors.SampleAggregator;
import org.opensearch.performanceanalyzer.rca.stats.emitters.ISampler;

public class JvmFreeMem implements ISampler {
    @Override
    public void sample(SampleAggregator sampleCollector) {
        sampleCollector.updateStat(
                JvmMetrics.JVM_FREE_MEM_SAMPLER, "", Runtime.getRuntime().freeMemory());
    }
}
