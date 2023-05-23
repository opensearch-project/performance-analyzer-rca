/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.collector;


import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.commons.metrics.ExceptionsAndErrors;
import org.opensearch.performanceanalyzer.commons.stats.CommonStats;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.EmptyFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.NodeConfigFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.NonLeafNode;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaGraphMetrics;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

/**
 * Cluster level node config collector that collect node config settings from each node and store
 * them into the {@link NodeConfigCache}
 */
public class NodeConfigClusterCollector extends NonLeafNode<EmptyFlowUnit> {

    private static final Logger LOG = LogManager.getLogger(NodeConfigClusterCollector.class);
    private final NodeConfigCollector nodeConfigCollector;

    public NodeConfigClusterCollector(final NodeConfigCollector nodeConfigCollector) {
        super(0, 5);
        this.nodeConfigCollector = nodeConfigCollector;
    }

    /**
     * read and parse the NodeConfigFlowUnit. retrieve the list of configs from the flowunit and
     * update the cache entries that are associated with the NodeKey + config type
     */
    private void addNodeLevelConfigs() {
        List<NodeConfigFlowUnit> flowUnits = nodeConfigCollector.getFlowUnits();
        for (NodeConfigFlowUnit flowUnit : flowUnits) {
            if (flowUnit.isEmpty() || !flowUnit.hasResourceSummary()) {
                continue;
            }
            HotNodeSummary nodeSummary = flowUnit.getSummary();
            NodeKey nodeKey = new NodeKey(nodeSummary.getNodeID(), nodeSummary.getHostAddress());
            NodeConfigCache nodeConfigCache = getAppContext().getNodeConfigCache();
            flowUnit.getConfigList()
                    .forEach(
                            resource -> {
                                double value = flowUnit.readConfig(resource);
                                if (!Double.isNaN(value)) {
                                    nodeConfigCache.put(nodeKey, resource, value);
                                }
                            });
        }
    }

    @Override
    public EmptyFlowUnit operate() {
        addNodeLevelConfigs();
        return new EmptyFlowUnit(System.currentTimeMillis());
    }

    @Override
    public void generateFlowUnitListFromLocal(FlowUnitOperationArgWrapper args) {
        LOG.debug("Collector: Executing fromLocal: {}", name());
        long startTime = System.currentTimeMillis();

        try {
            this.operate();
        } catch (Exception ex) {
            LOG.error("Collector: Exception in operate", ex);
            CommonStats.ERRORS_AND_EXCEPTIONS_AGGREGATOR.updateStat(
                    ExceptionsAndErrors.EXCEPTION_IN_OPERATE, name(), 1);
        }
        long duration = System.currentTimeMillis() - startTime;

        CommonStats.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                RcaGraphMetrics.GRAPH_NODE_OPERATE_CALL, this.name(), duration);
    }

    /** NodeConfigClusterCollector does not have downstream nodes and does not emit flow units */
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
}
