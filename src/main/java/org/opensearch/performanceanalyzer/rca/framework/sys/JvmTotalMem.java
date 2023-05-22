/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.sys;


import org.opensearch.performanceanalyzer.commons.stats.SampleAggregator;
import org.opensearch.performanceanalyzer.rca.framework.metrics.JvmMetrics;
import org.opensearch.performanceanalyzer.rca.stats.emitters.ISampler;

public class JvmTotalMem implements ISampler {
    @Override
    public void sample(SampleAggregator sampleCollector) {
        sampleCollector.updateStat(
                JvmMetrics.JVM_TOTAL_MEM_SAMPLER, "", Runtime.getRuntime().totalMemory());
    }
}
