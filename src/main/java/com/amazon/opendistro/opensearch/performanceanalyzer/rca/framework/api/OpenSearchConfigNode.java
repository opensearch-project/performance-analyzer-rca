/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazon.opendistro.opensearch.performanceanalyzer.rca.framework.api;


import com.amazon.opendistro.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import com.amazon.opendistro.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.framework.api.flow_units.NodeConfigFlowUnit;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.framework.core.NonLeafNode;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.framework.metrics.ExceptionsAndErrors;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.framework.metrics.RcaGraphMetrics;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
