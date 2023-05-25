/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders;


import com.google.common.annotations.VisibleForTesting;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.commons.stats.CommonStats;
import org.opensearch.performanceanalyzer.commons.stats.metrics.ExceptionsAndErrors;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.actions.ActionListener;
import org.opensearch.performanceanalyzer.decisionmaker.actions.FlipFlopDetector;
import org.opensearch.performanceanalyzer.decisionmaker.actions.TimedFlipFlopDetector;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.collator.Collator;
import org.opensearch.performanceanalyzer.rca.framework.core.NonLeafNode;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaGraphMetrics;
import org.opensearch.performanceanalyzer.rca.persistence.PublisherEventsPersistor;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;

public class Publisher extends NonLeafNode<EmptyFlowUnit> {

    private static final Logger LOG = LogManager.getLogger(Publisher.class);

    private Collator collator;
    private FlipFlopDetector flipFlopDetector;
    private boolean isMuted = false;
    private List<ActionListener> actionListeners;

    public Publisher(int evalIntervalSeconds, Collator collator) {
        super(0, evalIntervalSeconds);
        this.collator = collator;
        this.actionListeners = new ArrayList<>();
        // TODO please bring in guice so we can configure this with DI
        this.flipFlopDetector = new TimedFlipFlopDetector(1, TimeUnit.HOURS);
    }

    @Override
    public EmptyFlowUnit operate() {
        return new EmptyFlowUnit(Instant.now().toEpochMilli());
    }

    /** Publish the decisions to action listeners and persist actions. */
    public void compute(FlowUnitOperationArgWrapper args) {
        // TODO: Need to add dampening, avoidance etc.
        List<Action> actionsPublished = new ArrayList<>();
        if (!collator.getFlowUnits().isEmpty()) {
            Decision decision = collator.getFlowUnits().get(0);
            for (Action action : decision.getActions()) {
                if (!flipFlopDetector.isFlipFlop(action)) {
                    flipFlopDetector.recordAction(action);
                    for (ActionListener listener : actionListeners) {
                        listener.actionPublished(action);
                    }
                    actionsPublished.add(action);
                }
            }
        }
        // Persist actions to sqlite
        PublisherEventsPersistor persistor = new PublisherEventsPersistor(args.getPersistable());
        persistor.persistAction(actionsPublished, Instant.now().toEpochMilli());
    }

    @Override
    public void generateFlowUnitListFromLocal(FlowUnitOperationArgWrapper args) {
        LOG.debug("Publisher: Executing fromLocal: {}", name());
        long startTime = System.currentTimeMillis();

        try {
            this.compute(args);
        } catch (Exception ex) {
            LOG.error("Publisher: Exception in compute", ex);
            CommonStats.ERRORS_AND_EXCEPTIONS_AGGREGATOR.updateStat(
                    ExceptionsAndErrors.EXCEPTION_IN_COMPUTE, name(), 1);
        }
        long duration = System.currentTimeMillis() - startTime;

        CommonStats.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                RcaGraphMetrics.GRAPH_NODE_OPERATE_CALL, this.name(), duration);
    }

    /**
     * Register an action listener with Publisher
     *
     * <p>The listener is notified whenever an action is published
     */
    public void addActionListener(ActionListener listener) {
        actionListeners.add(listener);
    }

    /** Publisher does not have downstream nodes and does not emit flow units */
    @Override
    public void persistFlowUnit(FlowUnitOperationArgWrapper args) {
        assert true;
    }

    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        assert true;
    }

    @Override
    public void handleNodeMuted() {
        assert true;
    }

    @VisibleForTesting
    protected FlipFlopDetector getFlipFlopDetector() {
        return this.flipFlopDetector;
    }
}
