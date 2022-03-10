/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.jvmsizing;


import java.util.concurrent.TimeUnit;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.SlidingWindow;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.SlidingWindowData;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaVerticesMetrics;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;
import org.opensearch.performanceanalyzer.rca.store.rca.OldGenRca;

public class OldGenReclamationRca extends OldGenRca<ResourceFlowUnit<HotResourceSummary>> {

    private static final long EVAL_INTERVAL_IN_S = 5;
    private static final double DEFAULT_TARGET_UTILIZATION_AFTER_GC = 75.0d;
    private static final int DEFAULT_RCA_EVALUATION_INTERVAL_IN_S = 60;

    private final MinOldGenSlidingWindow minOldGenSlidingWindow;
    private final SlidingWindow<SlidingWindowData> gcEventsSlidingWindow;

    private HotResourceSummary prevSummary;
    private ResourceContext prevContext;
    private double targetHeapUtilizationAfterGc;
    private int rcaEvaluationIntervalInS;
    private long rcaPeriod;
    private int samples;

    public OldGenReclamationRca(
            final Metric heapUsed,
            final Metric heapMax,
            final Metric gcEvent,
            final Metric gcType) {
        this(
                heapUsed,
                heapMax,
                gcEvent,
                gcType,
                DEFAULT_TARGET_UTILIZATION_AFTER_GC,
                DEFAULT_RCA_EVALUATION_INTERVAL_IN_S);
    }

    public OldGenReclamationRca(
            final Metric heapUsed,
            final Metric heapMax,
            final Metric gcEvent,
            final Metric gcType,
            final double targetHeapUtilizationAfterGc,
            final int rcaEvaluationIntervalInS) {
        super(EVAL_INTERVAL_IN_S, heapUsed, heapMax, gcEvent, gcType);
        this.targetHeapUtilizationAfterGc = targetHeapUtilizationAfterGc;
        this.rcaEvaluationIntervalInS = rcaEvaluationIntervalInS;
        this.rcaPeriod = rcaEvaluationIntervalInS / EVAL_INTERVAL_IN_S;
        this.samples = 0;
        this.minOldGenSlidingWindow = new MinOldGenSlidingWindow(1, TimeUnit.MINUTES);
        this.gcEventsSlidingWindow = new SlidingWindow<>(1, TimeUnit.MINUTES);
        this.prevContext = new ResourceContext(Resources.State.UNKNOWN);
        this.prevSummary = null;
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
            // return an empty flow unit, we don't want to tune JVM when the collector is not CMS.
            return new ResourceFlowUnit<>(System.currentTimeMillis());
        }
        samples++;
        double oldGenMax = getMaxOldGenSizeOrDefault(Double.MAX_VALUE);
        double oldGenUsed = getOldGenUsedOrDefault(0d);
        double gcEvents = getFullGcEventsOrDefault(0d);
        long currTime = System.currentTimeMillis();
        minOldGenSlidingWindow.next(new SlidingWindowData(currTime, oldGenUsed));
        gcEventsSlidingWindow.next(new SlidingWindowData(currTime, gcEvents));

        if (samples == rcaPeriod) {
            samples = 0;
            double events = gcEventsSlidingWindow.readSum();
            if (events >= 1) {
                double threshold = targetHeapUtilizationAfterGc / 100d * oldGenMax;
                HotResourceSummary summary = null;
                ResourceContext context = null;
                if (minOldGenSlidingWindow.readMin() > threshold) {
                    summary =
                            new HotResourceSummary(
                                    ResourceUtil.FULL_GC_EFFECTIVENESS,
                                    targetHeapUtilizationAfterGc,
                                    minOldGenSlidingWindow.readMin(),
                                    rcaEvaluationIntervalInS);
                    context = new ResourceContext(Resources.State.UNHEALTHY);
                    PerformanceAnalyzerApp.RCA_VERTICES_METRICS_AGGREGATOR.updateStat(
                            RcaVerticesMetrics.OLD_GEN_RECLAMATION_INEFFECTIVE, "", 1);
                    prevSummary = summary;
                    prevContext = context;
                    return new ResourceFlowUnit<>(currTime, context, summary);
                } else {
                    summary =
                            new HotResourceSummary(
                                    ResourceUtil.FULL_GC_EFFECTIVENESS,
                                    targetHeapUtilizationAfterGc,
                                    minOldGenSlidingWindow.readMin(),
                                    rcaEvaluationIntervalInS);
                    context = new ResourceContext(Resources.State.HEALTHY);
                }

                prevSummary = summary;
                prevContext = context;

                return new ResourceFlowUnit<>(currTime, context, summary);
            }
        }
        return new ResourceFlowUnit<>(currTime, prevContext, prevSummary);
    }
}
