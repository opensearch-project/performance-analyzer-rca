/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.samplers;


import java.util.Objects;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ReaderMetrics;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.stats.collectors.SampleAggregator;
import org.opensearch.performanceanalyzer.rca.stats.emitters.ISampler;
import org.opensearch.performanceanalyzer.reader.ReaderMetricsProcessor;

public class BatchMetricsEnabledSampler implements ISampler {
    private final AppContext appContext;

    public BatchMetricsEnabledSampler(final AppContext appContext) {
        Objects.requireNonNull(appContext);
        this.appContext = appContext;
    }

    @Override
    public void sample(SampleAggregator sampleCollector) {
        sampleCollector.updateStat(
                ReaderMetrics.BATCH_METRICS_ENABLED, "", isBatchMetricsEnabled() ? 1 : 0);
    }

    boolean isBatchMetricsEnabled() {
        InstanceDetails currentNode = appContext.getMyInstanceDetails();
        if (currentNode != null && currentNode.getIsMaster()) {
            return ReaderMetricsProcessor.getInstance().getBatchMetricsEnabled();
        }
        return false;
    }
}
