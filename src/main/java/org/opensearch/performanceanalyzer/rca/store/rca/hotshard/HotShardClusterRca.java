/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.hotshard;


import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.rca.configs.HotShardClusterRcaConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotShardSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;

/**
 * This RCA is used to find hot shards per index in a cluster using the HotShardSummary sent from
 * each node via 'HotShardRca'. If the resource utilization is (threshold)% higher than the mean
 * resource utilization for the index, we declare the shard hot.
 */
public class HotShardClusterRca extends Rca<ResourceFlowUnit<HotClusterSummary>> {

    public static final String RCA_TABLE_NAME = HotShardClusterRca.class.getSimpleName();
    private static final Logger LOG = LogManager.getLogger(HotShardClusterRca.class);
    private static final int SLIDING_WINDOW_IN_SECONDS = 60;

    private double cpuUtilizationClusterThreshold;
    private double heapAllocRateClusterThreshold;

    private final Rca<ResourceFlowUnit<HotNodeSummary>> hotShardRca;
    private int rcaPeriod;
    private int counter;
    private Set<String> unhealthyNodes;

    // Guava Table with Row: 'Index_Name', Column: 'NodeShardKey', Cell Value: 'Value'
    // TODO: Use the fact that we're getting at max topK*2 consumers from each node and perform further optimization.
    private Table<String, NodeShardKey, Double> cpuUtilizationInfoTable;
    private Table<String, NodeShardKey, Double> heapAllocRateInfoTable;

    public <R extends Rca<ResourceFlowUnit<HotNodeSummary>>> HotShardClusterRca(
            final int rcaPeriod, final R hotShardRca) {
        super(5);
        this.hotShardRca = hotShardRca;
        this.rcaPeriod = rcaPeriod;
        this.counter = 0;
        this.unhealthyNodes = new HashSet<>();
        this.cpuUtilizationInfoTable = HashBasedTable.create();
        this.heapAllocRateInfoTable = HashBasedTable.create();
        this.cpuUtilizationClusterThreshold =
                HotShardClusterRcaConfig.DEFAULT_CPU_UTILIZATION_CLUSTER_THRESHOLD;
        this.heapAllocRateClusterThreshold =
                HotShardClusterRcaConfig.DEFAULT_HEAP_ALLOC_RATE_CLUSTER_THRESHOLD;
    }

    private void populateResourceInfoTable(
            String indexName,
            NodeShardKey nodeShardKey,
            double metricValue,
            Table<String, NodeShardKey, Double> metricMap) {
        if (null == metricMap.get(indexName, nodeShardKey)) {
            metricMap.put(indexName, nodeShardKey, metricValue);
        } else {
            double existingOccurence = metricMap.get(indexName, nodeShardKey);
            metricMap.put(indexName, nodeShardKey, existingOccurence + metricValue);
        }
    }

    private void consumeFlowUnit(ResourceFlowUnit<HotNodeSummary> resourceFlowUnit) {
        HotNodeSummary hotNodeSummary = resourceFlowUnit.getSummary();
        String nodeId = hotNodeSummary.getNodeID().toString();
        for (GenericSummary summary : hotNodeSummary.getNestedSummaryList()) {
            if (summary instanceof HotShardSummary) {
                HotShardSummary hotShardSummary = (HotShardSummary) summary;
                String indexName = hotShardSummary.getIndexName();
                NodeShardKey nodeShardKey = new NodeShardKey(nodeId, hotShardSummary.getShardId());

                populateResourceInfoTable(
                        indexName,
                        nodeShardKey,
                        hotShardSummary.getCpuUtilization(),
                        cpuUtilizationInfoTable);
                populateResourceInfoTable(
                        indexName,
                        nodeShardKey,
                        hotShardSummary.getHeapAllocRate(),
                        heapAllocRateInfoTable);
            }
        }
    }

    /**
     * Evaluates the threshold value for resource usage across shards for given index.
     *
     * @param perIndexShardInfo Resource usage across shards for given index
     * @param thresholdInPercentage Threshold for the resource in percentage
     */
    private double getThresholdValue(
            Map<NodeShardKey, Double> perIndexShardInfo, double thresholdInPercentage) {
        // To handle the outlier(s) in the data, using median instead of mean
        double[] perIndexShardUsage =
                perIndexShardInfo.values().stream().mapToDouble(usage -> usage).toArray();
        Arrays.sort(perIndexShardUsage);

        double median;
        int length = perIndexShardUsage.length;
        if (length % 2 != 0) {
            median = perIndexShardUsage[length / 2];
        } else {
            median = (perIndexShardUsage[(length - 1) / 2] + perIndexShardUsage[length / 2]) / 2.0;
        }
        return (median * (1 + thresholdInPercentage));
    }

    /**
     * Finds hot shard(s) across an index and creates HotResourceSummary for them.
     *
     * @param resourceInfoTable Guava Table with 'Index_Name', 'NodeShardKey' and 'UsageValue'
     * @param thresholdInPercentage Threshold for the resource in percentage
     * @param hotResourceSummaryList Summary List for hot shards
     * @param resource Resource message object defined in protobuf
     */
    private void findHotShardAndCreateSummary(
            Table<String, NodeShardKey, Double> resourceInfoTable,
            double thresholdInPercentage,
            List<HotResourceSummary> hotResourceSummaryList,
            Resource resource) {
        for (String indexName : resourceInfoTable.rowKeySet()) {
            Map<NodeShardKey, Double> perIndexShardInfo = resourceInfoTable.row(indexName);
            double thresholdValue = getThresholdValue(perIndexShardInfo, thresholdInPercentage);
            for (Map.Entry<NodeShardKey, Double> shardInfo : perIndexShardInfo.entrySet()) {
                if (shardInfo.getValue() > thresholdValue) {
                    // Shard Identifier is represented by "Node_ID Index_Name Shard_ID" string
                    String shardIdentifier =
                            String.join(
                                    " ",
                                    new String[] {
                                        shardInfo.getKey().getNodeId(),
                                        indexName,
                                        shardInfo.getKey().getShardId()
                                    });

                    // Add to hotResourceSummaryList
                    hotResourceSummaryList.add(
                            new HotResourceSummary(
                                    resource,
                                    thresholdValue,
                                    shardInfo.getValue(),
                                    SLIDING_WINDOW_IN_SECONDS,
                                    shardIdentifier));
                }
            }
        }
    }

    /**
     * Compare between the shard counterparts. Within an index, the shard which is (threshold)%
     * higher than the mean resource utilization is hot.
     *
     * <p>We are evaluating hot shards on 2 dimensions and if shard is hot in any of the 2
     * dimension, we declare it hot.
     */
    @Override
    public ResourceFlowUnit<HotClusterSummary> operate() {
        counter++;

        // Populate the Table, compiling the information per index
        final List<ResourceFlowUnit<HotNodeSummary>> resourceFlowUnits = hotShardRca.getFlowUnits();
        for (final ResourceFlowUnit<HotNodeSummary> resourceFlowUnit : resourceFlowUnits) {
            if (resourceFlowUnit.isEmpty()) {
                continue;
            }

            if (resourceFlowUnit.getResourceContext().isUnhealthy()) {
                unhealthyNodes.add(resourceFlowUnit.getSummary().getNodeID().toString());
                consumeFlowUnit(resourceFlowUnit);
            }
        }

        if (counter >= rcaPeriod) {
            List<HotResourceSummary> hotShardSummaryList = new ArrayList<>();
            ResourceContext context;
            HotClusterSummary summary =
                    new HotClusterSummary(getAllClusterInstances().size(), unhealthyNodes.size());

            // We evaluate hot shards individually on both dimensions
            findHotShardAndCreateSummary(
                    cpuUtilizationInfoTable,
                    cpuUtilizationClusterThreshold,
                    hotShardSummaryList,
                    ResourceUtil.CPU_USAGE);

            findHotShardAndCreateSummary(
                    heapAllocRateInfoTable,
                    heapAllocRateClusterThreshold,
                    hotShardSummaryList,
                    ResourceUtil.HEAP_ALLOC_RATE);

            if (hotShardSummaryList.isEmpty()) {
                context = new ResourceContext(Resources.State.HEALTHY);
            } else {
                context = new ResourceContext(Resources.State.UNHEALTHY);

                InstanceDetails instanceDetails = getInstanceDetails();
                HotNodeSummary nodeSummary =
                        new HotNodeSummary(
                                instanceDetails.getInstanceId(), instanceDetails.getInstanceIp());
                for (HotResourceSummary hotResourceSummary : hotShardSummaryList) {
                    nodeSummary.appendNestedSummary(hotResourceSummary);
                }
                summary.appendNestedSummary(nodeSummary);
                LOG.debug("rca: Hot Shards Identified: {}", hotShardSummaryList);
            }

            // reset the variables
            counter = 0;
            this.unhealthyNodes.clear();
            this.cpuUtilizationInfoTable.clear();
            this.heapAllocRateInfoTable.clear();
            LOG.debug("Hot Shard Cluster RCA Context :  " + context.toString());
            return new ResourceFlowUnit<>(System.currentTimeMillis(), context, summary, true);
        } else {
            LOG.debug("Empty FlowUnit returned for Hot Shard CLuster RCA");
            return new ResourceFlowUnit<>(System.currentTimeMillis());
        }
    }

    /**
     * read threshold values from rca.conf
     *
     * @param conf RcaConf object
     */
    @Override
    public void readRcaConf(RcaConf conf) {
        HotShardClusterRcaConfig configObj = conf.getHotShardClusterRcaConfig();
        cpuUtilizationClusterThreshold = configObj.getCpuUtilizationClusterThreshold();
        heapAllocRateClusterThreshold = configObj.getHeapAllocRateClusterThreshold();
    }

    /**
     * This is a cluster level RCA vertex which by definition can not be serialize/de-serialized
     * over gRPC.
     */
    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        throw new IllegalArgumentException(
                name() + "'s generateFlowUnitListFromWire() should not " + "be required.");
    }
}
