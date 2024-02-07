/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.commons.stats.ServiceMetrics;
import org.opensearch.performanceanalyzer.rca.framework.core.NonLeafNode;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ExceptionsAndErrors;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaGraphMetrics;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;

/**
 * Generic base class outlining the basic structure of a Decider.
 *
 * <p>Deciders process observation summaries from RCA nodes to generate Action recommendations. Each
 * decider operates independent of other deciders. It subscribes to relevant RCAs and Metrics, to
 * create a candidate set of actions for unhealthy nodes in the cluster (as suggested by its
 * upstream RCAs).
 *
 * <p>{@link Decider} implementations should override operate() and return a {@link Decision} based
 * on the decider's evaluations.
 *
 * <p>A Decision can contain multiple actions. Each Action contains details about the OpenSearch
 * nodes it touches and its impact on key resource metrics.
 */
public abstract class Decider extends NonLeafNode<Decision> {

    private static final Logger LOG = LogManager.getLogger(Decider.class);
    protected final int
            decisionFrequency; // Measured in terms of number of evaluationIntervalPeriods
    protected RcaConf rcaConf;

    public Decider(long evalIntervalSeconds, int decisionFrequency) {
        super(0, evalIntervalSeconds);
        this.decisionFrequency = decisionFrequency;
        this.rcaConf = null;
    }

    public abstract String name();

    @Override
    public void generateFlowUnitListFromLocal(FlowUnitOperationArgWrapper args) {
        LOG.debug("decider: Executing fromLocal: {}", name());
        long startTime = System.currentTimeMillis();

        Decision decision;
        try {
            decision = this.operate();
        } catch (Exception ex) {
            LOG.error("decider: Exception in operate", ex);
            ServiceMetrics.ERRORS_AND_EXCEPTIONS_AGGREGATOR.updateStat(
                    ExceptionsAndErrors.EXCEPTION_IN_OPERATE, name(), 1);
            decision = new Decision(System.currentTimeMillis(), this.name());
        }
        long duration = System.currentTimeMillis() - startTime;

        ServiceMetrics.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                RcaGraphMetrics.GRAPH_NODE_OPERATE_CALL, this.name(), duration);

        setLocalFlowUnit(decision);
    }

    @Override
    public void persistFlowUnit(FlowUnitOperationArgWrapper args) {
        // TODO: Persist Decisions taken by deciders to support queryable APIs and general
        // bookkeeping
        // This is a no-op for now
        assert true;
    }

    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        throw new IllegalArgumentException(name() + ": not expected to be called over the wire");
    }

    @Override
    public void handleNodeMuted() {
        setLocalFlowUnit(new Decision(System.currentTimeMillis(), this.name()));
    }

    @Override
    public abstract Decision operate();

    /**
     * read threshold values from rca.conf
     *
     * @param conf RcaConf object
     */
    @Override
    public void readRcaConf(RcaConf conf) {
        rcaConf = conf;
    }
}
