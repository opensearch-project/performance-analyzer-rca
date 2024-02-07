/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.net;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.commons.collectors.StatsCollector;
import org.opensearch.performanceanalyzer.commons.stats.metrics.StatExceptionCode;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;

/**
 * An intermediate buffer that holds flow units received for different vertices from across the
 * cluster.
 */
public class ReceivedFlowUnitStore {

    private static final Logger LOG = LogManager.getLogger(ReceivedFlowUnitStore.class);

    /** Map of vertex to a queue of flow units received for that vertex. */
    private ConcurrentMap<String, BlockingQueue<FlowUnitMessage>> flowUnitMap =
            new ConcurrentHashMap<>();

    /** The per vertex flow unit queue size. */
    private final int perNodeFlowUnitQSize;

    public ReceivedFlowUnitStore() {
        this(RcaConsts.DEFAULT_PER_NODE_FLOWUNIT_Q_SIZE);
    }

    public ReceivedFlowUnitStore(final int perNodeFlowUnitQSize) {
        this.perNodeFlowUnitQSize = perNodeFlowUnitQSize;
    }

    /**
     * Adds the received flow unit from the network to a dedicated queue for holding flow units for
     * this particular vertex. This queue is then consumed by the wirehopper when the time comes to
     * execute the vertex.
     *
     * @param graphNode The vertex for which we need to store the remote flow units for.
     * @param flowUnitMessage The actual flow unit message protobuf object that we received from the
     *     network that needs to be stored.
     * @return true if the enqueue operation succeeded, false if the queue was full and we have to
     *     drop the flow unit.
     */
    public boolean enqueue(final String graphNode, final FlowUnitMessage flowUnitMessage) {
        flowUnitMap.computeIfAbsent(graphNode, s -> new ArrayBlockingQueue<>(perNodeFlowUnitQSize));
        BlockingQueue<FlowUnitMessage> existingQueue = flowUnitMap.get(graphNode);
        boolean retValue = existingQueue.offer(flowUnitMessage);
        if (!retValue) {
            LOG.warn("Dropped flow unit because per vertex queue is full");
            StatsCollector.instance()
                    .logException(StatExceptionCode.RCA_VERTEX_RX_BUFFER_FULL_ERROR);
        }

        return retValue;
    }

    /**
     * Drain the flow units enqueued for the vertex.
     *
     * @param graphNode The vertex whose flow units needed to be drained.
     * @return An immutable list containing the flow units received from the network for the vertex.
     */
    public ImmutableList<FlowUnitMessage> drainNode(final String graphNode) {
        LOG.debug("Draining flow units for vertex: {}", graphNode);
        final List<FlowUnitMessage> tempList = new ArrayList<>();
        BlockingQueue<FlowUnitMessage> existing = flowUnitMap.get(graphNode);
        if (existing == null) {
            LOG.debug("Nothing in the FlowUnitStore for vertex: {}", graphNode);
            return ImmutableList.of();
        }

        existing.drainTo(tempList);
        LOG.debug("Available flow units for vertex: {}, flowUnits: {}", graphNode, tempList);

        return ImmutableList.copyOf(tempList);
    }

    /** Drains out all the flow units for all nodes. */
    public List<FlowUnitMessage> drainAll() {
        List<FlowUnitMessage> drained = new ArrayList<>();
        for (final String graphNode : flowUnitMap.keySet()) {
            ImmutableList<FlowUnitMessage> messages = drainNode(graphNode);
            drained.addAll(messages);
        }
        return drained;
    }
}
