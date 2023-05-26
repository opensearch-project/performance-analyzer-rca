/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.samplers;


import java.util.Objects;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.commons.stats.collectors.SampleAggregator;
import org.opensearch.performanceanalyzer.commons.stats.emitters.ISampler;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaRuntimeMetrics;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;

public class RcaEnabledSampler implements ISampler {
    private final AppContext appContext;

    RcaEnabledSampler(final AppContext appContext) {
        Objects.requireNonNull(appContext);
        this.appContext = appContext;
    }

    @Override
    public void sample(SampleAggregator sampleCollector) {
        sampleCollector.updateStat(RcaRuntimeMetrics.RCA_ENABLED, isRcaEnabled() ? 1 : 0);
    }

    boolean isRcaEnabled() {
        InstanceDetails currentNode = appContext.getMyInstanceDetails();
        if (currentNode != null && currentNode.getIsClusterManager()) {
            return PerformanceAnalyzerApp.getRcaController().isRcaEnabled();
        }
        return false;
    }
}
