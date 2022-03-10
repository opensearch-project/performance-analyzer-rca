/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api;


import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.NodeConfigFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.core.NonLeafNode;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ExceptionsAndErrors;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaGraphMetrics;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;

/**
 * this is a base class for node(vertex) in RCA graph that reads configuration settings from
 * OpenSearch.
 */
public abstract class OpenSearchConfigNode extends NonLeafNode<NodeConfigFlowUnit> {

    private static final Logger LOG = LogManager.getLogger(OpenSearchConfigNode.class);

    public OpenSearchConfigNode() {
        super(0, 5);
    }

    /**
     * fetch flowunits from local graph node
     *
     * @param args The wrapper around the flow unit operation.
     */
    @Override
    public void generateFlowUnitListFromLocal(FlowUnitOperationArgWrapper args) {
        long startTime = System.currentTimeMillis();
        NodeConfigFlowUnit result;
        try {
            result = this.operate();
        } catch (Exception ex) {
            LOG.error("Exception in operate.", ex);
            PerformanceAnalyzerApp.ERRORS_AND_EXCEPTIONS_AGGREGATOR.updateStat(
                    ExceptionsAndErrors.EXCEPTION_IN_OPERATE, name(), 1);
            result = new NodeConfigFlowUnit(System.currentTimeMillis());
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        PerformanceAnalyzerApp.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                RcaGraphMetrics.GRAPH_NODE_OPERATE_CALL, this.name(), duration);

        setLocalFlowUnit(result);
    }

    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        final List<FlowUnitMessage> flowUnitMessages =
                args.getWireHopper().readFromWire(args.getNode());
        List<NodeConfigFlowUnit> flowUnitList = new ArrayList<>();
        LOG.debug("rca: Executing fromWire: {}", this.getClass().getSimpleName());
        for (FlowUnitMessage flowUnitMessage : flowUnitMessages) {
            flowUnitList.add(NodeConfigFlowUnit.buildFlowUnitFromWrapper(flowUnitMessage));
        }
        setFlowUnits(flowUnitList);
    }

    @Override
    public void handleNodeMuted() {
        setLocalFlowUnit(new NodeConfigFlowUnit(System.currentTimeMillis()));
    }

    /**
     * OpenSearchConfig metrics are not intended to be persisted
     *
     * @param args FlowUnitOperationArgWrapper
     */
    @Override
    public void persistFlowUnit(FlowUnitOperationArgWrapper args) {
        assert true;
    }
}
