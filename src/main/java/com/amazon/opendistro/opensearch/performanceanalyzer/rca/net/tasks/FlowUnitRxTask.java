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
 * Copyright 2019-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amazon.opendistro.opensearch.performanceanalyzer.rca.net.tasks;


import com.amazon.opendistro.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import com.amazon.opendistro.opensearch.performanceanalyzer.collectors.StatsCollector;
import com.amazon.opendistro.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.framework.metrics.RcaGraphMetrics;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.net.NodeStateManager;
import com.amazon.opendistro.opensearch.performanceanalyzer.rca.net.ReceivedFlowUnitStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Task that processes received flow units. */
public class FlowUnitRxTask implements Runnable {

    private static final Logger LOG = LogManager.getLogger(FlowUnitRxTask.class);
    /** Node state manager instance. */
    private final NodeStateManager nodeStateManager;

    /** The buffer for holding received flow units till they are consumed by the vertices. */
    private final ReceivedFlowUnitStore receivedFlowUnitStore;

    /** The flow unit message object to buffer. */
    private final FlowUnitMessage flowUnitMessage;

    public FlowUnitRxTask(
            final NodeStateManager nodeStateManager,
            final ReceivedFlowUnitStore receivedFlowUnitStore,
            final FlowUnitMessage flowUnitMessage) {
        this.nodeStateManager = nodeStateManager;
        this.receivedFlowUnitStore = receivedFlowUnitStore;
        this.flowUnitMessage = flowUnitMessage;
    }

    /**
     * Updates the per vertex flow unit collection.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        final InstanceDetails.Id host = new InstanceDetails.Id(flowUnitMessage.getNode());
        final String vertex = flowUnitMessage.getGraphNode();

        nodeStateManager.updateReceiveTime(host, vertex, System.currentTimeMillis());
        LOG.debug("rca: [pub-rx]: {} <- {}", vertex, host);
        if (!receivedFlowUnitStore.enqueue(vertex, flowUnitMessage)) {
            LOG.warn(
                    "Dropped a flow unit because the vertex buffer was full for vertex: {}",
                    vertex);
            StatsCollector.instance().logMetric(RcaConsts.VERTEX_BUFFER_FULL_METRIC);
        }

        PerformanceAnalyzerApp.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                RcaGraphMetrics.RCA_NODES_FU_CONSUME_COUNT, vertex, 1);
    }
}
