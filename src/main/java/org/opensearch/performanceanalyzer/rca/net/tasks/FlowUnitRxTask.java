/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.net.tasks;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaGraphMetrics;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.net.NodeStateManager;
import org.opensearch.performanceanalyzer.rca.net.ReceivedFlowUnitStore;

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
        }
        PerformanceAnalyzerApp.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                RcaGraphMetrics.RCA_NODES_FU_CONSUME_COUNT, vertex, 1);
    }
}
