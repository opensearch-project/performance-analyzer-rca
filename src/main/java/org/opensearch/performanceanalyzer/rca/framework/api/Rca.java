/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.commons.stats.ServiceMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.core.NonLeafNode;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ExceptionsAndErrors;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaGraphMetrics;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;

public abstract class Rca<T extends ResourceFlowUnit> extends NonLeafNode<T> {
    private static final Logger LOG = LogManager.getLogger(Rca.class);

    public Rca(long evaluationIntervalSeconds) {
        super(0, evaluationIntervalSeconds);
    }

    /**
     * fetch flowunits from local graph node
     *
     * @param args The wrapper around the flow unit operation.
     */
    @Override
    public void generateFlowUnitListFromLocal(FlowUnitOperationArgWrapper args) {
        LOG.debug("rca: Executing fromLocal: {}", this.getClass().getSimpleName());

        long startTime = System.currentTimeMillis();

        T result;
        try {
            result = this.operate();
        } catch (Exception ex) {
            LOG.error("Exception in operate.", ex);
            ServiceMetrics.ERRORS_AND_EXCEPTIONS_AGGREGATOR.updateStat(
                    ExceptionsAndErrors.EXCEPTION_IN_OPERATE, name(), 1);
            result = (T) T.generic();
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        ServiceMetrics.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                RcaGraphMetrics.GRAPH_NODE_OPERATE_CALL, this.name(), duration);

        setLocalFlowUnit(result);
    }

    /**
     * This method specifies what needs to be done when the current node is muted for throwing
     * exceptions.
     */
    @Override
    public void handleNodeMuted() {
        setLocalFlowUnit((T) T.generic());
    }

    @Override
    public void persistFlowUnit(FlowUnitOperationArgWrapper args) {
        long startTime = System.currentTimeMillis();
        for (final T flowUnit : getFlowUnits()) {
            try {
                args.getPersistable().write(this, flowUnit);
            } catch (Exception ex) {
                LOG.error("Caught exception while persisting node: {}", args.getNode().name(), ex);
                ServiceMetrics.ERRORS_AND_EXCEPTIONS_AGGREGATOR.updateStat(
                        ExceptionsAndErrors.EXCEPTION_IN_PERSIST, name(), 1);
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        ServiceMetrics.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                RcaGraphMetrics.RCA_PERSIST_CALL, this.name(), duration);
    }
}
