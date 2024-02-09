/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.jvmsizing;

import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.commons.stats.ServiceMetrics;
import org.opensearch.performanceanalyzer.rca.configs.HighOldGenOccupancyRcaConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.SlidingWindow;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.SlidingWindowData;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaVerticesMetrics;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;
import org.opensearch.performanceanalyzer.rca.store.rca.OldGenRca;

public class HighOldGenOccupancyRca extends OldGenRca<ResourceFlowUnit<HotResourceSummary>> {

    private static final Logger LOG = LogManager.getLogger(HighOldGenOccupancyRca.class);
    private static final long EVAL_INTERVAL_IN_S = 5;
    private static final int B_TO_MB = 1024 * 1024;

    private final Metric heapUsed;
    private final Metric heapMax;
    private final Metric gcType;
    private final SlidingWindow<SlidingWindowData> oldGenUtilizationSlidingWindow;

    private long heapUtilizationThreshold;
    private long rcaEvaluationIntervalInS;
    private long rcaSamplesBeforeEval;
    private long samples;

    private ResourceContext previousContext;
    private HotResourceSummary previousSummary;

    /**
     * Create HighOldGenOccupancyRca with default values.
     *
     * @param heapMax The heapMax metric.
     * @param heapUsed The heapUsed metric.
     */
    public HighOldGenOccupancyRca(final Metric heapMax, final Metric heapUsed, Metric gcType) {
        this(
                heapMax,
                heapUsed,
                gcType,
                HighOldGenOccupancyRcaConfig.DEFAULT_UTILIZATION,
                HighOldGenOccupancyRcaConfig.DEFAULT_EVALUATION_INTERVAL_IN_S);
    }

    public HighOldGenOccupancyRca(
            final Metric heapMax,
            final Metric heapUsed,
            Metric gcType,
            final long heapUtilizationThreshold,
            final long rcaEvaluationIntervalInS) {
        super(EVAL_INTERVAL_IN_S, heapUsed, heapMax, null, gcType);
        this.oldGenUtilizationSlidingWindow = new SlidingWindow<>(1, TimeUnit.MINUTES);
        this.heapUsed = heapUsed;
        this.heapMax = heapMax;
        this.gcType = gcType;
        this.heapUtilizationThreshold = heapUtilizationThreshold;
        this.rcaEvaluationIntervalInS = rcaEvaluationIntervalInS;
        this.rcaSamplesBeforeEval = rcaEvaluationIntervalInS / EVAL_INTERVAL_IN_S;
        this.samples = 0;
        this.previousContext = new ResourceContext(Resources.State.UNKNOWN);
        this.previousSummary = null;
    }

    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        throw new UnsupportedOperationException(
                "generateFlowUnitListFromWire should not be called "
                        + "for node-local rca: "
                        + args.getNode().name());
    }

    @Override
    public ResourceFlowUnit<HotResourceSummary> operate() {
        if (!isOldGenCollectorCMS()) {
            // return an empty flow unit. We don't want to tune the JVM when the collector is not
            // CMS.
            return new ResourceFlowUnit<>(System.currentTimeMillis());
        }
        samples++;
        addToSlidingWindow();
        if (samples == rcaSamplesBeforeEval) {
            samples = 0;
            return evaluateAndEmit();
        }

        return new ResourceFlowUnit<>(System.currentTimeMillis(), previousContext, previousSummary);
    }

    private ResourceFlowUnit<HotResourceSummary> evaluateAndEmit() {
        long currTime = System.currentTimeMillis();
        double averageUtilizationPercentage = oldGenUtilizationSlidingWindow.readAvg();
        ResourceContext context = new ResourceContext(Resources.State.HEALTHY);
        HotResourceSummary summary =
                new HotResourceSummary(
                        ResourceUtil.OLD_GEN_HEAP_USAGE,
                        (double) heapUtilizationThreshold,
                        averageUtilizationPercentage,
                        (int) rcaEvaluationIntervalInS);
        if (averageUtilizationPercentage >= heapUtilizationThreshold) {
            ServiceMetrics.RCA_VERTICES_METRICS_AGGREGATOR.updateStat(
                    RcaVerticesMetrics.OLD_GEN_OVER_OCCUPIED, 1);
            context = new ResourceContext(Resources.State.UNHEALTHY);
        }
        this.previousSummary = summary;
        this.previousContext = context;
        return new ResourceFlowUnit<>(currTime, context, summary);
    }

    private void addToSlidingWindow() {
        double oldGenUsed = getOldGenUsedOrDefault(0d);
        double maxOldGen = getMaxOldGenSizeOrDefault(Double.MAX_VALUE);

        if (maxOldGen == 0d) {
            ServiceMetrics.RCA_VERTICES_METRICS_AGGREGATOR.updateStat(
                    RcaVerticesMetrics.INVALID_OLD_GEN_SIZE, 1);
            LOG.info("Max Old Gen capacity cannot be 0. Skipping.");
            return;
        }

        this.oldGenUtilizationSlidingWindow.next(
                new SlidingWindowData(System.currentTimeMillis(), (oldGenUsed / maxOldGen) * 100d));
    }

    @Override
    public void readRcaConf(RcaConf conf) {
        final HighOldGenOccupancyRcaConfig config = conf.getHighOldGenOccupancyRcaConfig();
        this.rcaEvaluationIntervalInS = config.getEvaluationIntervalInS();
        this.heapUtilizationThreshold = config.getHeapUtilizationThreshold();
        this.rcaSamplesBeforeEval = rcaEvaluationIntervalInS / EVAL_INTERVAL_IN_S;
    }
}
