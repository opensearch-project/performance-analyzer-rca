/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api;


import java.util.Collections;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.commons.stats.ServiceMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.SymptomFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.core.NonLeafNode;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ExceptionsAndErrors;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaGraphMetrics;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;

public abstract class Symptom extends NonLeafNode<SymptomFlowUnit> {
    private static final Logger LOG = LogManager.getLogger(Symptom.class);

    public Symptom(long evaluationIntervalSeconds) {
        super(0, evaluationIntervalSeconds);
    }

    public void generateFlowUnitListFromLocal(FlowUnitOperationArgWrapper args) {
        LOG.debug("rca: Executing handleRca: {}", this.getClass().getSimpleName());

        long startTime = System.currentTimeMillis();
        SymptomFlowUnit result;
        try {
            result = this.operate();
        } catch (Exception ex) {
            LOG.error("[MOCHI]: Key Value is: {}", this.name());
            ServiceMetrics.ERRORS_AND_EXCEPTIONS_AGGREGATOR.updateStat(
                    ExceptionsAndErrors.EXCEPTION_IN_OPERATE, this.name(), 1);
            LOG.error("Exception caught during operate", ex);
            result = SymptomFlowUnit.generic();
        }
        long endTime = System.currentTimeMillis();
        long durationMillis = endTime - startTime;

        ServiceMetrics.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                RcaGraphMetrics.GRAPH_NODE_OPERATE_CALL, this.name(), durationMillis);

        setFlowUnits(Collections.singletonList(result));
    }

    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        // TODO
    }

    /**
     * This method specifies what needs to be done when the current node is muted for throwing
     * exceptions.
     */
    @Override
    public void handleNodeMuted() {
        setLocalFlowUnit(SymptomFlowUnit.generic());
    }

    /**
     * Persists a flow unit.
     *
     * @param args The arg wrapper.
     */
    @Override
    public void persistFlowUnit(FlowUnitOperationArgWrapper args) {}
}
