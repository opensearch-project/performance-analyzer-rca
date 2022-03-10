/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders;


import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.bucket.BasicBucketCalculator;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.bucket.BucketCalculator;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.bucket.UsageBucket;
import org.opensearch.performanceanalyzer.rca.store.rca.HighHeapUsageClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;

public abstract class HeapBasedDecider extends Decider {
    private static final Logger LOG = LogManager.getLogger(HeapBasedDecider.class);
    private static final String OLD_GEN_TUNABLE_KEY = "old-gen";
    private static final ResourceEnum DECIDING_HEAP_RESOURCE_TYPE = ResourceEnum.OLD_GEN;
    public static final ImmutableMap<UsageBucket, Double> DEFAULT_HEAP_USAGE_THRESHOLDS =
            ImmutableMap.<UsageBucket, Double>builder()
                    .put(UsageBucket.UNDER_UTILIZED, 10.0)
                    .put(UsageBucket.HEALTHY_WITH_BUFFER, 60.0)
                    .put(UsageBucket.HEALTHY, 80.0)
                    .build();

    private HighHeapUsageClusterRca highHeapUsageClusterRca;

    public HeapBasedDecider(
            long evalIntervalSeconds,
            int decisionFrequency,
            HighHeapUsageClusterRca highHeapUsageClusterRca) {
        super(evalIntervalSeconds, decisionFrequency);
        this.highHeapUsageClusterRca = highHeapUsageClusterRca;
    }

    /**
     * The Queue and Cache deciders should only be able to suggest increase of the queue size or
     * increase of cache size if the Java heap can sustain more live objects in it without
     * de-gradation. What is an acceptable heap usage limit to determine this, comes from the
     * bucketization object in rca.conf. We compare the oldGen usage percent reported by the
     * HighHeapUsage RCA to determine that.
     *
     * @param nodeKey The NodeKey we are trying to make a decision for.
     * @return return if the OldGen heap is under-utilized or healthy and yet more can be consumed,
     *     return true; or false otherwise.
     */
    protected boolean canUseMoreHeap(NodeKey nodeKey) {
        // we add action only if heap is under-utilized or healthy and yet more can be consumed.
        for (ResourceFlowUnit<HotClusterSummary> clusterSummary :
                highHeapUsageClusterRca.getFlowUnits()) {
            if (clusterSummary.hasResourceSummary()) {
                for (HotNodeSummary nodeSummary :
                        clusterSummary.getSummary().getHotNodeSummaryList()) {
                    NodeKey thisNode =
                            new NodeKey(nodeSummary.getNodeID(), nodeSummary.getHostAddress());
                    if (thisNode.equals(nodeKey)) {
                        for (HotResourceSummary hotResourceSummary :
                                nodeSummary.getHotResourceSummaryList()) {
                            Resource resource = hotResourceSummary.getResource();
                            if (resource.getResourceEnum() == DECIDING_HEAP_RESOURCE_TYPE) {
                                return checkIfResourceCanBeTuned(hotResourceSummary.getValue());
                            }
                        }
                    }
                }
            }
        }
        // If the heapUsageRca does not have the oldGen details, we assume that we can use more
        // heap.
        return true;
    }

    private boolean checkIfResourceCanBeTuned(double oldGenUsedRatio) {
        double oldGenUsedPercent = oldGenUsedRatio * 100;
        BucketCalculator bucketCalculator;
        try {
            bucketCalculator = rcaConf.getBucketizationSettings(OLD_GEN_TUNABLE_KEY);
        } catch (Exception jsonEx) {
            bucketCalculator = new BasicBucketCalculator(DEFAULT_HEAP_USAGE_THRESHOLDS);
            LOG.debug("rca.conf does not have bucketization limits specified. Using default map.");
        }
        UsageBucket bucket = bucketCalculator.compute(oldGenUsedPercent);
        LOG.debug(
                "Value ({}) bucketized to {}, using {}",
                oldGenUsedPercent,
                bucket.toString(),
                bucketCalculator.toString());

        boolean canTune = false;
        if (bucket == UsageBucket.UNDER_UTILIZED || bucket == UsageBucket.HEALTHY_WITH_BUFFER) {
            canTune = true;
        }
        return canTune;
    }
}
