/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.persistence;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;

@SuppressWarnings("unchecked")
public class NetPersistor {
    private static final Logger LOG = LogManager.getLogger(NetPersistor.class);
    private static final int PER_GRAPH_NODE_FLOW_UNIT_QUEUE_CAPACITY = 200;
    ConcurrentMap<String, BlockingQueue<FlowUnitMessage>> graphNodeToFlowUnitMap =
            new ConcurrentHashMap<>();

    public List<FlowUnitMessage> read(final String graphNode) {
        if (graphNodeToFlowUnitMap.containsKey(graphNode)) {
            final BlockingQueue<FlowUnitMessage> flowUnitQueue =
                    graphNodeToFlowUnitMap.get(graphNode);
            final List<FlowUnitMessage> returnList = new ArrayList<>();
            flowUnitQueue.drainTo(returnList);
            return returnList;
        }

        return new ArrayList<>();
    }

    public void write(final String graphNode, final FlowUnitMessage flowUnitMessage) {
        if (flowUnitMessage == null) {
            LOG.debug("receive a null flowunit message. Dropping the flow unit.");
            return;
        }
        graphNodeToFlowUnitMap.putIfAbsent(
                graphNode, new ArrayBlockingQueue<>(PER_GRAPH_NODE_FLOW_UNIT_QUEUE_CAPACITY));
        final BlockingQueue<FlowUnitMessage> flowUnitQueue = graphNodeToFlowUnitMap.get(graphNode);

        if (!flowUnitQueue.offer(flowUnitMessage)) {
            LOG.debug("Failed to add flow unit to the buffer. Dropping the flow unit.");
        }
    }
}
