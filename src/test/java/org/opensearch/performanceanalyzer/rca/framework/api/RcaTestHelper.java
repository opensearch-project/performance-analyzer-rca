/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api;

import java.time.Clock;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.opensearch.performanceanalyzer.grpc.HotShardSummaryMessage.CriteriaEnum;
import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotShardSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericSummary;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;
import org.opensearch.performanceanalyzer.rca.store.collector.NodeConfigCache;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

public class RcaTestHelper<T extends GenericSummary> extends Rca<ResourceFlowUnit<T>> {
    private Clock clock;
    private String rcaName;

    public RcaTestHelper() {
        super(5);
        this.clock = Clock.systemUTC();
        this.rcaName = name();
    }

    public RcaTestHelper(String rcaName) {
        this();
        this.rcaName = rcaName;
    }

    public void mockFlowUnit(ResourceFlowUnit<T> flowUnit) {
        this.flowUnits = Collections.singletonList(flowUnit);
    }

    public void mockFlowUnit(ResourceFlowUnit<T>... flowUnit) {
        this.flowUnits = Arrays.asList(flowUnit);
    }

    public void mockFlowUnit() {
        this.flowUnits =
                Collections.singletonList((ResourceFlowUnit<T>) ResourceFlowUnit.generic());
    }

    public void mockFlowUnits(List<ResourceFlowUnit<T>> flowUnitList) {
        this.flowUnits = flowUnitList;
    }

    public double readConfig(NodeKey nodeKey, Resource resource) throws IllegalArgumentException {
        NodeConfigCache nodeConfigCache = getAppContext().getNodeConfigCache();
        return nodeConfigCache.get(nodeKey, resource);
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String name() {
        return rcaName;
    }

    @Override
    public ResourceFlowUnit<T> operate() {
        return null;
    }

    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {}

    public static ResourceFlowUnit<HotNodeSummary> generateFlowUnit(
            Resource type, String nodeID, Resources.State healthy) {
        HotResourceSummary resourceSummary = new HotResourceSummary(type, 10, 5, 60);
        HotNodeSummary nodeSummary =
                new HotNodeSummary(
                        new InstanceDetails.Id(nodeID), new InstanceDetails.Ip("127.0.0.0"));
        nodeSummary.appendNestedSummary(resourceSummary);
        return new ResourceFlowUnit<>(
                System.currentTimeMillis(), new ResourceContext(healthy), nodeSummary);
    }

    public static ResourceFlowUnit<HotNodeSummary> generateFlowUnit(
            Resource type, String nodeID, String hostAddress, Resources.State healthy) {
        HotResourceSummary resourceSummary = new HotResourceSummary(type, 10, 5, 60);
        HotNodeSummary nodeSummary =
                new HotNodeSummary(
                        new InstanceDetails.Id(nodeID), new InstanceDetails.Ip(hostAddress));
        nodeSummary.appendNestedSummary(resourceSummary);
        return new ResourceFlowUnit<>(
                System.currentTimeMillis(), new ResourceContext(healthy), nodeSummary);
    }

    public static ResourceFlowUnit<HotNodeSummary> generateFlowUnit(
            Resource type,
            String nodeID,
            String hostAddress,
            Resources.State healthy,
            long timestamp) {
        HotResourceSummary resourceSummary = new HotResourceSummary(type, 10, 5, 60);
        HotNodeSummary nodeSummary =
                new HotNodeSummary(
                        new InstanceDetails.Id(nodeID), new InstanceDetails.Ip(hostAddress));
        nodeSummary.appendNestedSummary(resourceSummary);
        return new ResourceFlowUnit<>(timestamp, new ResourceContext(healthy), nodeSummary);
    }

    /** Create HotNodeSummary flow unit with multiple unhealthy resources */
    public static ResourceFlowUnit<HotNodeSummary> generateFlowUnit(
            String nodeID, String hostAddress, Resources.State healthy, Resource... resources) {
        HotNodeSummary nodeSummary =
                new HotNodeSummary(
                        new InstanceDetails.Id(nodeID), new InstanceDetails.Ip(hostAddress));
        for (Resource resource : resources) {
            HotResourceSummary resourceSummary = new HotResourceSummary(resource, 10, 5, 60);
            nodeSummary.appendNestedSummary(resourceSummary);
        }
        return new ResourceFlowUnit<>(
                System.currentTimeMillis(), new ResourceContext(healthy), nodeSummary);
    }

    public static ResourceFlowUnit<HotNodeSummary> generateFlowUnitForHotShard(
            String indexName,
            String shardId,
            String nodeID,
            double cpuUtilization,
            CriteriaEnum criteria,
            Resources.State health) {
        HotShardSummary hotShardSummary = new HotShardSummary(indexName, shardId, nodeID, 60);
        hotShardSummary.setCriteria(criteria);
        hotShardSummary.setCpuUtilization(cpuUtilization);
        HotNodeSummary nodeSummary =
                new HotNodeSummary(
                        new InstanceDetails.Id(nodeID), new InstanceDetails.Ip("127.0.0.0"));
        nodeSummary.appendNestedSummary(hotShardSummary);
        return new ResourceFlowUnit<>(
                System.currentTimeMillis(), new ResourceContext(health), nodeSummary);
    }
}
