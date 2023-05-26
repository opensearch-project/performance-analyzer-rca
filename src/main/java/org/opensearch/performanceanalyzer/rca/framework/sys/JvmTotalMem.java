/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.sys;


import org.opensearch.performanceanalyzer.commons.stats.collectors.SampleAggregator;
import org.opensearch.performanceanalyzer.commons.stats.emitters.ISampler;
import org.opensearch.performanceanalyzer.rca.framework.metrics.JvmMetrics;

public class JvmTotalMem implements ISampler {
    @Override
    public void sample(SampleAggregator sampleCollector) {
        sampleCollector.updateStat(
                JvmMetrics.JVM_TOTAL_MEM_SAMPLER, Runtime.getRuntime().totalMemory());
    }
}
