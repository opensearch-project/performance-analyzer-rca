/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.scheduler;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.rca.framework.core.Stats;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaGraphMetrics;

public class GraphNodeOperations {
    private static final Logger LOG = LogManager.getLogger(GraphNodeOperations.class);

    static void readFromLocal(FlowUnitOperationArgWrapper args) {
        if (Stats.getInstance().isNodeMuted(args.getNode().name())) {
            args.getNode().handleNodeMuted();
            return;
        }
        args.getNode().generateFlowUnitListFromLocal(args);
        args.getNode().persistFlowUnit(args);
        PerformanceAnalyzerApp.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                RcaGraphMetrics.NUM_NODES_EXECUTED_LOCALLY, "", 1);
    }

    // This is the abstraction for when the data arrives on the wire from a remote dependency.
    static void readFromWire(FlowUnitOperationArgWrapper args) {
        // flowUnits.forEach(i -> LOG.info("rca: Read from wire: {}", i));
        args.getNode().generateFlowUnitListFromWire(args);
        PerformanceAnalyzerApp.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                RcaGraphMetrics.NUM_NODES_EXECUTED_REMOTELY, "", 1);
    }
}
