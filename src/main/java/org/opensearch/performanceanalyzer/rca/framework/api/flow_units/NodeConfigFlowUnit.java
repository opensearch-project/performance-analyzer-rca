/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.flow_units;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage.SummaryOneofCase;
import org.opensearch.performanceanalyzer.grpc.HotNodeSummaryMessage;
import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

/** a flowunit type to carry OpenSearch node configurations (queue/cache capacities, etc.) */
public class NodeConfigFlowUnit extends ResourceFlowUnit<HotNodeSummary> {

    private final HashMap<Resource, HotResourceSummary> configMap;

    public NodeConfigFlowUnit(long timeStamp) {
        super(timeStamp);
        this.configMap = new HashMap<>();
    }

    public NodeConfigFlowUnit(long timeStamp, NodeKey nodeKey) {
        super(timeStamp, new ResourceContext(Resources.State.HEALTHY), null, false);
        this.setSummary(new HotNodeSummary(nodeKey.getNodeId(), nodeKey.getHostAddress()));
        this.configMap = new HashMap<>();
    }

    /**
     * Add new config setting to flowunit
     *
     * @param resource config setting type
     * @param value config setting value
     */
    public void addConfig(Resource resource, double value) {
        HotResourceSummary configSummary = new HotResourceSummary(resource, Double.NaN, value, 0);
        configMap.put(resource, configSummary);
    }

    /**
     * Add new config setting to flowunit
     *
     * @param configSummary config setting summary object
     */
    public void addConfig(HotResourceSummary configSummary) {
        configMap.put(configSummary.getResource(), configSummary);
    }

    /**
     * check if the config setting exist in flowunit
     *
     * @param resource config setting type
     * @return if config exist
     */
    public boolean hasConfig(Resource resource) {
        return configMap.containsKey(resource);
    }

    /**
     * read the config value of the config setting from flowunit
     *
     * @param resource config setting type
     * @return config setting value
     */
    public double readConfig(Resource resource) {
        HotResourceSummary configSummary = configMap.getOrDefault(resource, null);
        if (configSummary == null) {
            return Double.NaN;
        }
        return configSummary.getValue();
    }

    /**
     * get list of config settings that this flowunit contains
     *
     * @return list of config settings
     */
    public List<Resource> getConfigList() {
        return new ArrayList<>(configMap.keySet());
    }

    @Override
    public boolean isEmpty() {
        return configMap.isEmpty();
    }

    /**
     * convert NodeConfigFlowUnit into ResourceFlowUnit {@link ResourceFlowUnit} before serializing
     * the flowunit object to gRPC protobuf message
     *
     * @param graphNode vertex name in RCA graph
     * @param node opensearch node ID
     * @return gRPC protobuf message
     */
    @Override
    public FlowUnitMessage buildFlowUnitMessage(
            final String graphNode, final InstanceDetails.Id node) {
        // append resources stored in configMap to HotNodeSummary
        this.configMap
                .values()
                .forEach(resourceSummary -> this.getSummary().appendNestedSummary(resourceSummary));
        return super.buildFlowUnitMessage(graphNode, node);
    }

    /** build NodeConfigFlowUnit from the protobuf message */
    public static NodeConfigFlowUnit buildFlowUnitFromWrapper(final FlowUnitMessage message) {
        NodeConfigFlowUnit nodeConfigFlowUnit;
        if (message.getSummaryOneofCase() == SummaryOneofCase.HOTNODESUMMARY) {
            HotNodeSummaryMessage nodeSummaryMessage = message.getHotNodeSummary();
            NodeKey nodeKey =
                    new NodeKey(
                            new InstanceDetails.Id(nodeSummaryMessage.getNodeID()),
                            new InstanceDetails.Ip(nodeSummaryMessage.getHostAddress()));
            nodeConfigFlowUnit = new NodeConfigFlowUnit(message.getTimeStamp(), nodeKey);
            if (nodeSummaryMessage.hasHotResourceSummaryList()) {
                for (int i = 0;
                        i
                                < nodeSummaryMessage
                                        .getHotResourceSummaryList()
                                        .getHotResourceSummaryCount();
                        i++) {
                    nodeConfigFlowUnit.addConfig(
                            HotResourceSummary.buildHotResourceSummaryFromMessage(
                                    nodeSummaryMessage
                                            .getHotResourceSummaryList()
                                            .getHotResourceSummary(i)));
                }
            }
        } else {
            nodeConfigFlowUnit = new NodeConfigFlowUnit(message.getTimeStamp());
        }
        return nodeConfigFlowUnit;
    }
}
