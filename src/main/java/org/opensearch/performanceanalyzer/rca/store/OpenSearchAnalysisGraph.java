/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.AdmissionControlDecider;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.CacheHealthDecider;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.Publisher;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.QueueHealthDecider;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.collator.Collator;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.jvm.HeapHealthDecider;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.plugins.PluginController;
import org.opensearch.performanceanalyzer.plugins.PluginControllerConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.AnalysisGraph;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Bitset_Memory;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.CPU_Utilization;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Cache_FieldData_Eviction;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Cache_FieldData_Size;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Cache_Max_Size;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Cache_Query_Size;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Cache_Request_Eviction;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Cache_Request_Hit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Cache_Request_Size;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.GC_Collection_Event;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.GC_Collection_Time;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.GC_Type;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Heap_Max;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Heap_Used;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.IndexWriter_Memory;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.ThreadPool_QueueCapacity;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.ThreadPool_RejectedReqs;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Thread_Blocked_Time;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Thread_Waited_Time;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.VersionMap_Memory;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.Node;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.ShardStore;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;
import org.opensearch.performanceanalyzer.rca.store.collector.NodeConfigClusterCollector;
import org.opensearch.performanceanalyzer.rca.store.collector.NodeConfigCollector;
import org.opensearch.performanceanalyzer.rca.store.metric.AggregateMetric;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.byShard.AvgCpuUtilByShardsMetricBasedTemperatureCalculator;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.byShard.CpuUtilByShardsMetricBasedTemperatureCalculator;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.byShard.HeapAllocRateByShardAvgTemperatureCalculator;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.byShard.HeapAllocRateByShardTemperatureCalculator;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.byShard.ShardSizeAvgTemperatureCalculator;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.byShard.ShardSizeMetricBasedTemperatureCalculator;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.capacity.HeapAllocRateTotalTemperatureCalculator;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.capacity.ShardTotalDiskUsageTemperatureCalculator;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.capacity.TotalCpuUtilForTotalNodeMetric;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.shardIndependent.HeapAllocRateShardIndependentTemperatureCalculator;
import org.opensearch.performanceanalyzer.rca.store.metric.temperature.shardIndependent.ShardIndependentTemperatureCalculatorCpuUtilMetric;
import org.opensearch.performanceanalyzer.rca.store.rca.HighHeapUsageClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.HotNodeClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.HotNodeRca;
import org.opensearch.performanceanalyzer.rca.store.rca.admissioncontrol.AdmissionControlClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.admissioncontrol.AdmissionControlRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cache.FieldDataCacheRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cache.ShardRequestCacheRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.FieldDataCacheClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.QueueRejectionClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.ShardRequestCacheClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.hot_node.HighCpuRca;
import org.opensearch.performanceanalyzer.rca.store.rca.hot_node.ThreadMetricsRca;
import org.opensearch.performanceanalyzer.rca.store.rca.hotheap.HighHeapUsageOldGenRca;
import org.opensearch.performanceanalyzer.rca.store.rca.hotheap.HighHeapUsageYoungGenRca;
import org.opensearch.performanceanalyzer.rca.store.rca.hotshard.HotShardClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.hotshard.HotShardRca;
import org.opensearch.performanceanalyzer.rca.store.rca.jvmsizing.HighOldGenOccupancyRca;
import org.opensearch.performanceanalyzer.rca.store.rca.jvmsizing.LargeHeapClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.jvmsizing.OldGenContendedRca;
import org.opensearch.performanceanalyzer.rca.store.rca.jvmsizing.OldGenReclamationRca;
import org.opensearch.performanceanalyzer.rca.store.rca.searchbackpressure.SearchBackPressureRCA;
import org.opensearch.performanceanalyzer.rca.store.rca.temperature.NodeTemperatureRca;
import org.opensearch.performanceanalyzer.rca.store.rca.temperature.dimension.CpuUtilDimensionTemperatureRca;
import org.opensearch.performanceanalyzer.rca.store.rca.temperature.dimension.HeapAllocRateTemperatureRca;
import org.opensearch.performanceanalyzer.rca.store.rca.temperature.dimension.ShardSizeDimensionTemperatureRca;
import org.opensearch.performanceanalyzer.rca.store.rca.threadpool.QueueRejectionRca;

public class OpenSearchAnalysisGraph extends AnalysisGraph {

    private static final Logger LOG = LogManager.getLogger(OpenSearchAnalysisGraph.class);
    private static final int EVALUATION_INTERVAL_SECONDS = 5;
    private static final int SECONDS_IN_MIN = 60;
    // 1 minute. RCA_PERIOD is measured as number of EVALUATION_INTERVAL_SECONDS
    private static final int RCA_PERIOD = SECONDS_IN_MIN / EVALUATION_INTERVAL_SECONDS;

    @Override
    public void construct() {
        Metric heapUsed = new Heap_Used(EVALUATION_INTERVAL_SECONDS);
        Metric gcEvent = new GC_Collection_Event(EVALUATION_INTERVAL_SECONDS);
        Thread_Blocked_Time threadBlockedTime =
                new Thread_Blocked_Time(EVALUATION_INTERVAL_SECONDS);
        Thread_Waited_Time threadWaitedTime = new Thread_Waited_Time(EVALUATION_INTERVAL_SECONDS);
        Heap_Max heapMax = new Heap_Max(EVALUATION_INTERVAL_SECONDS);
        Metric gc_Collection_Time = new GC_Collection_Time(EVALUATION_INTERVAL_SECONDS);
        GC_Type gcType = new GC_Type(EVALUATION_INTERVAL_SECONDS);
        Metric cpuUtilizationGroupByOperation =
                new AggregateMetric(
                        1,
                        CPU_Utilization.NAME,
                        AggregateMetric.AggregateFunction.SUM,
                        MetricsDB.AVG,
                        AllMetrics.CommonDimension.OPERATION.toString());

        // SearchBackpressure Metric
        // Metric searchbp_Stats = new Searchbp_Stats(EVALUATION_INTERVAL_SECONDS);

        heapUsed.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        gcEvent.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        gcType.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        heapMax.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        gc_Collection_Time.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        cpuUtilizationGroupByOperation.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        threadBlockedTime.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        threadWaitedTime.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        // searchbp_Stats.addTag(
        //         RcaConsts.RcaTagConstants.TAG_LOCUS,
        //         RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);

        addLeaf(heapUsed);
        addLeaf(gcEvent);
        addLeaf(gcType);
        addLeaf(heapMax);
        addLeaf(gc_Collection_Time);
        addLeaf(cpuUtilizationGroupByOperation);
        addLeaf(threadBlockedTime);
        addLeaf(threadWaitedTime);
        // addLeaf(searchbp_Stats);

        // add node stats metrics
        List<Metric> nodeStatsMetrics = constructNodeStatsMetrics();

        Rca<ResourceFlowUnit<HotResourceSummary>> highHeapUsageOldGenRca =
                new HighHeapUsageOldGenRca(
                        RCA_PERIOD, heapUsed, gcEvent, heapMax, nodeStatsMetrics);
        highHeapUsageOldGenRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        List<Node<?>> upstream = new ArrayList<>(Arrays.asList(heapUsed, gcEvent, heapMax));
        upstream.addAll(nodeStatsMetrics);
        highHeapUsageOldGenRca.addAllUpstreams(upstream);

        Rca<ResourceFlowUnit<HotResourceSummary>> highHeapUsageYoungGenRca =
                new HighHeapUsageYoungGenRca(
                        RCA_PERIOD, heapUsed, gc_Collection_Time, gcEvent, gcType);
        highHeapUsageYoungGenRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        highHeapUsageYoungGenRca.addAllUpstreams(
                Arrays.asList(heapUsed, gc_Collection_Time, gcEvent, gcType));

        Rca<ResourceFlowUnit<HotResourceSummary>> highCpuRca =
                new HighCpuRca(RCA_PERIOD, cpuUtilizationGroupByOperation);
        highCpuRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        highCpuRca.addAllUpstreams(Collections.singletonList(cpuUtilizationGroupByOperation));

        // Use EVALUATION_INTERVAL_SECONDS instead of RCA_PERIOD which resolved to 12 seconds.
        // This is resulting in this RCA not getting executed in every 5 seconds.
        Rca<ResourceFlowUnit<HotNodeSummary>> threadMetricsRca =
                new ThreadMetricsRca(
                        threadBlockedTime, threadWaitedTime, EVALUATION_INTERVAL_SECONDS);
        threadMetricsRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        threadMetricsRca.addAllUpstreams(List.of(threadBlockedTime, threadWaitedTime));

        Rca<ResourceFlowUnit<HotNodeSummary>> hotJVMNodeRca =
                new HotNodeRca(
                        RCA_PERIOD, highHeapUsageOldGenRca, highHeapUsageYoungGenRca, highCpuRca);
        hotJVMNodeRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        hotJVMNodeRca.addAllUpstreams(
                Arrays.asList(
                        highHeapUsageOldGenRca,
                        highHeapUsageYoungGenRca,
                        highCpuRca,
                        threadMetricsRca));

        HighHeapUsageClusterRca highHeapUsageClusterRca =
                new HighHeapUsageClusterRca(RCA_PERIOD, hotJVMNodeRca);
        highHeapUsageClusterRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_CLUSTER_MANAGER_NODE);
        highHeapUsageClusterRca.addAllUpstreams(Collections.singletonList(hotJVMNodeRca));
        highHeapUsageClusterRca.addTag(
                RcaConsts.RcaTagConstants.TAG_AGGREGATE_UPSTREAM,
                RcaConsts.RcaTagConstants.LOCUS_DATA_NODE);

        HotNodeClusterRca hotNodeClusterRca = new HotNodeClusterRca(RCA_PERIOD, hotJVMNodeRca);
        hotNodeClusterRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_CLUSTER_MANAGER_NODE);
        hotNodeClusterRca.addAllUpstreams(Collections.singletonList(hotJVMNodeRca));

        final HighOldGenOccupancyRca oldGenOccupancyRca =
                new HighOldGenOccupancyRca(heapMax, heapUsed, gcType);
        oldGenOccupancyRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        oldGenOccupancyRca.addAllUpstreams(Arrays.asList(heapMax, heapUsed, gcType));

        final OldGenReclamationRca oldGenReclamationRca =
                new OldGenReclamationRca(heapUsed, heapMax, gcEvent, gcType);
        oldGenReclamationRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        oldGenReclamationRca.addAllUpstreams(Arrays.asList(heapUsed, heapMax, gcEvent, gcType));

        final OldGenContendedRca oldGenContendedRca =
                new OldGenContendedRca(oldGenOccupancyRca, oldGenReclamationRca);
        oldGenContendedRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        oldGenContendedRca.addAllUpstreams(Arrays.asList(oldGenOccupancyRca, oldGenReclamationRca));

        final LargeHeapClusterRca largeHeapClusterRca = new LargeHeapClusterRca(oldGenContendedRca);
        largeHeapClusterRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_CLUSTER_MANAGER_NODE);
        largeHeapClusterRca.addAllUpstreams(Collections.singletonList(oldGenContendedRca));
        largeHeapClusterRca.addTag(
                RcaConsts.RcaTagConstants.TAG_AGGREGATE_UPSTREAM,
                RcaConsts.RcaTagConstants.LOCUS_DATA_NODE);

        // Heap Health Decider
        HeapHealthDecider heapHealthDecider =
                new HeapHealthDecider(RCA_PERIOD, highHeapUsageClusterRca, largeHeapClusterRca);
        heapHealthDecider.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_CLUSTER_MANAGER_NODE);
        heapHealthDecider.addAllUpstreams(
                Arrays.asList(highHeapUsageClusterRca, largeHeapClusterRca));

        /* Queue Rejection RCAs
         */
        // TODO: Refactor this monolithic function
        Metric threadpool_RejectedReqs = new ThreadPool_RejectedReqs(EVALUATION_INTERVAL_SECONDS);
        threadpool_RejectedReqs.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        addLeaf(threadpool_RejectedReqs);

        // Node level queue rejection RCA
        QueueRejectionRca queueRejectionNodeRca =
                new QueueRejectionRca(RCA_PERIOD, threadpool_RejectedReqs);
        queueRejectionNodeRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        queueRejectionNodeRca.addAllUpstreams(Collections.singletonList(threadpool_RejectedReqs));

        // Cluster level queue rejection RCA
        QueueRejectionClusterRca queueRejectionClusterRca =
                new QueueRejectionClusterRca(RCA_PERIOD, queueRejectionNodeRca);
        queueRejectionClusterRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_CLUSTER_MANAGER_NODE);
        queueRejectionClusterRca.addAllUpstreams(Collections.singletonList(queueRejectionNodeRca));
        queueRejectionClusterRca.addTag(
                RcaConsts.RcaTagConstants.TAG_AGGREGATE_UPSTREAM,
                RcaConsts.RcaTagConstants.LOCUS_DATA_NODE);

        // Queue Health Decider
        QueueHealthDecider queueHealthDecider =
                new QueueHealthDecider(
                        EVALUATION_INTERVAL_SECONDS,
                        12,
                        queueRejectionClusterRca,
                        highHeapUsageClusterRca);
        queueHealthDecider.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_CLUSTER_MANAGER_NODE);
        queueHealthDecider.addAllUpstreams(
                Arrays.asList(queueRejectionClusterRca, highHeapUsageClusterRca));

        // Node Config Collector
        ThreadPool_QueueCapacity queueCapacity = new ThreadPool_QueueCapacity();
        queueCapacity.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        addLeaf(queueCapacity);

        Cache_Max_Size cacheMaxSize = new Cache_Max_Size(EVALUATION_INTERVAL_SECONDS);
        cacheMaxSize.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        addLeaf(cacheMaxSize);

        NodeConfigCollector nodeConfigCollector =
                new NodeConfigCollector(RCA_PERIOD, queueCapacity, cacheMaxSize, heapMax);
        nodeConfigCollector.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        nodeConfigCollector.addAllUpstreams(Arrays.asList(queueCapacity, cacheMaxSize, heapMax));
        NodeConfigClusterCollector nodeConfigClusterCollector =
                new NodeConfigClusterCollector(nodeConfigCollector);
        nodeConfigClusterCollector.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_CLUSTER_MANAGER_NODE);
        nodeConfigClusterCollector.addAllUpstreams(Collections.singletonList(nodeConfigCollector));
        nodeConfigClusterCollector.addTag(
                RcaConsts.RcaTagConstants.TAG_AGGREGATE_UPSTREAM,
                RcaConsts.RcaTagConstants.LOCUS_DATA_NODE);

        // Field Data Cache RCA
        Metric fieldDataCacheEvictions = new Cache_FieldData_Eviction(EVALUATION_INTERVAL_SECONDS);
        fieldDataCacheEvictions.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        addLeaf(fieldDataCacheEvictions);

        Metric fieldDataCacheSizeGroupByOperation =
                new AggregateMetric(
                        EVALUATION_INTERVAL_SECONDS,
                        Cache_FieldData_Size.NAME,
                        AggregateMetric.AggregateFunction.SUM,
                        MetricsDB.MAX,
                        AllMetrics.ShardStatsDerivedDimension.INDEX_NAME.toString());
        fieldDataCacheSizeGroupByOperation.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        addLeaf(fieldDataCacheSizeGroupByOperation);

        FieldDataCacheRca fieldDataCacheNodeRca =
                new FieldDataCacheRca(
                        RCA_PERIOD, fieldDataCacheEvictions, fieldDataCacheSizeGroupByOperation);
        fieldDataCacheNodeRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        fieldDataCacheNodeRca.addAllUpstreams(
                Arrays.asList(fieldDataCacheEvictions, fieldDataCacheSizeGroupByOperation));

        FieldDataCacheClusterRca fieldDataCacheClusterRca =
                new FieldDataCacheClusterRca(RCA_PERIOD, fieldDataCacheNodeRca);
        fieldDataCacheClusterRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_CLUSTER_MANAGER_NODE);
        fieldDataCacheClusterRca.addAllUpstreams(Collections.singletonList(fieldDataCacheNodeRca));
        fieldDataCacheClusterRca.addTag(
                RcaConsts.RcaTagConstants.TAG_AGGREGATE_UPSTREAM,
                RcaConsts.RcaTagConstants.LOCUS_DATA_NODE);

        // Shard Request Cache RCA
        Metric shardRequestCacheEvictions = new Cache_Request_Eviction(EVALUATION_INTERVAL_SECONDS);
        shardRequestCacheEvictions.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        addLeaf(shardRequestCacheEvictions);
        Metric shardRequestHits = new Cache_Request_Hit(EVALUATION_INTERVAL_SECONDS);
        shardRequestHits.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        addLeaf(shardRequestHits);

        Metric shardRequestCacheSizeGroupByOperation =
                new AggregateMetric(
                        EVALUATION_INTERVAL_SECONDS,
                        Cache_Request_Size.NAME,
                        AggregateMetric.AggregateFunction.SUM,
                        MetricsDB.MAX,
                        AllMetrics.ShardStatsDerivedDimension.INDEX_NAME.toString());
        shardRequestCacheSizeGroupByOperation.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        addLeaf(shardRequestCacheSizeGroupByOperation);

        ShardRequestCacheRca shardRequestCacheNodeRca =
                new ShardRequestCacheRca(
                        RCA_PERIOD,
                        shardRequestCacheEvictions,
                        shardRequestHits,
                        shardRequestCacheSizeGroupByOperation);
        shardRequestCacheNodeRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        shardRequestCacheNodeRca.addAllUpstreams(
                Arrays.asList(
                        shardRequestCacheEvictions,
                        shardRequestHits,
                        shardRequestCacheSizeGroupByOperation));

        ShardRequestCacheClusterRca shardRequestCacheClusterRca =
                new ShardRequestCacheClusterRca(RCA_PERIOD, shardRequestCacheNodeRca);
        shardRequestCacheClusterRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_CLUSTER_MANAGER_NODE);
        shardRequestCacheClusterRca.addAllUpstreams(
                Collections.singletonList(shardRequestCacheNodeRca));
        shardRequestCacheClusterRca.addTag(
                RcaConsts.RcaTagConstants.TAG_AGGREGATE_UPSTREAM,
                RcaConsts.RcaTagConstants.LOCUS_DATA_NODE);

        // Cache Health Decider
        CacheHealthDecider cacheHealthDecider =
                new CacheHealthDecider(
                        EVALUATION_INTERVAL_SECONDS,
                        12,
                        fieldDataCacheClusterRca,
                        shardRequestCacheClusterRca,
                        highHeapUsageClusterRca);
        cacheHealthDecider.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_CLUSTER_MANAGER_NODE);
        cacheHealthDecider.addAllUpstreams(
                Arrays.asList(
                        fieldDataCacheClusterRca,
                        shardRequestCacheClusterRca,
                        highHeapUsageClusterRca));

        // Search Back Pressure Service RCA
        final SearchBackPressureRCA searchBackPressureRCA =
                new SearchBackPressureRCA(heapMax, heapUsed, gcType);
        searchBackPressureRCA.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        searchBackPressureRCA.addAllUpstreams(Arrays.asList(heapMax, heapUsed, gcType));

        AdmissionControlDecider admissionControlDecider =
                buildAdmissionControlDecider(heapUsed, heapMax);

        constructShardResourceUsageGraph();

        constructResourceHeatMapGraph();

        // Collator - Collects actions from all deciders and aligns impact vectors
        Collator collator =
                new Collator(
                        queueHealthDecider,
                        cacheHealthDecider,
                        heapHealthDecider,
                        admissionControlDecider);
        collator.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_CLUSTER_MANAGER_NODE);
        collator.addAllUpstreams(
                Arrays.asList(
                        queueHealthDecider,
                        cacheHealthDecider,
                        heapHealthDecider,
                        admissionControlDecider));

        // Publisher - Executes decisions output from collator
        Publisher publisher = new Publisher(EVALUATION_INTERVAL_SECONDS, collator);
        publisher.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_CLUSTER_MANAGER_NODE);
        publisher.addAllUpstreams(Collections.singletonList(collator));

        // TODO: Refactor using DI to move out of construct method
        PluginControllerConfig pluginControllerConfig = new PluginControllerConfig();
        PluginController pluginController = new PluginController(pluginControllerConfig, publisher);
        pluginController.initPlugins();
    }

    private AdmissionControlDecider buildAdmissionControlDecider(Metric heapUsed, Metric heapMax) {
        AdmissionControlRca admissionControlRca =
                new AdmissionControlRca(RCA_PERIOD, heapUsed, heapMax);
        admissionControlRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS, RcaConsts.RcaTagConstants.LOCUS_DATA_NODE);
        admissionControlRca.addAllUpstreams(Arrays.asList(heapUsed, heapMax));

        AdmissionControlClusterRca admissionControlClusterRca =
                new AdmissionControlClusterRca(RCA_PERIOD, admissionControlRca);
        admissionControlClusterRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_CLUSTER_MANAGER_NODE);
        admissionControlClusterRca.addAllUpstreams(Collections.singletonList(admissionControlRca));
        admissionControlClusterRca.addTag(
                RcaConsts.RcaTagConstants.TAG_AGGREGATE_UPSTREAM,
                RcaConsts.RcaTagConstants.LOCUS_DATA_NODE);

        AdmissionControlDecider admissionControlDecider =
                new AdmissionControlDecider(
                        EVALUATION_INTERVAL_SECONDS, RCA_PERIOD, admissionControlClusterRca);
        admissionControlDecider.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_CLUSTER_MANAGER_NODE);
        admissionControlDecider.addAllUpstreams(
                Collections.singletonList(admissionControlClusterRca));

        return admissionControlDecider;
    }

    private void constructShardResourceUsageGraph() {
        Metric cpuUtilization = new CPU_Utilization(EVALUATION_INTERVAL_SECONDS);

        cpuUtilization.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);

        addLeaf(cpuUtilization);

        // High CPU Utilization RCA
        HotShardRca hotShardRca =
                new HotShardRca(EVALUATION_INTERVAL_SECONDS, RCA_PERIOD, cpuUtilization);
        hotShardRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        hotShardRca.addAllUpstreams(Arrays.asList(cpuUtilization));

        // Hot Shard Cluster RCA which consumes the above
        HotShardClusterRca hotShardClusterRca = new HotShardClusterRca(RCA_PERIOD, hotShardRca);
        hotShardClusterRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_CLUSTER_MANAGER_NODE);
        hotShardClusterRca.addAllUpstreams(Collections.singletonList(hotShardRca));
        hotShardClusterRca.addTag(
                RcaConsts.RcaTagConstants.TAG_AGGREGATE_UPSTREAM,
                RcaConsts.RcaTagConstants.LOCUS_DATA_NODE);
    }

    private List<Metric> constructNodeStatsMetrics() {
        List<Metric> nodeStatsMetrics =
                new ArrayList<Metric>() {
                    {
                        add(new Cache_FieldData_Size(EVALUATION_INTERVAL_SECONDS));
                        add(new Cache_Request_Size(EVALUATION_INTERVAL_SECONDS));
                        add(new Cache_Query_Size(EVALUATION_INTERVAL_SECONDS));
                        add(new IndexWriter_Memory(EVALUATION_INTERVAL_SECONDS));
                        add(new Bitset_Memory(EVALUATION_INTERVAL_SECONDS));
                        add(new VersionMap_Memory(EVALUATION_INTERVAL_SECONDS));
                    }
                };
        for (Metric metric : nodeStatsMetrics) {
            metric.addTag(
                    RcaConsts.RcaTagConstants.TAG_LOCUS,
                    RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
            addLeaf(metric);
        }
        return nodeStatsMetrics;
    }

    protected void constructResourceHeatMapGraph() {
        LOG.info("Constructing temperature profile RCA components");
        ShardStore shardStore = new ShardStore();

        HeapAllocRateByShardTemperatureCalculator heapAllocByShard =
                new HeapAllocRateByShardTemperatureCalculator();
        HeapAllocRateByShardAvgTemperatureCalculator heapAllocRateByShardAvg =
                new HeapAllocRateByShardAvgTemperatureCalculator();
        HeapAllocRateShardIndependentTemperatureCalculator shardIndependentHeapAllocRate =
                new HeapAllocRateShardIndependentTemperatureCalculator();
        HeapAllocRateTotalTemperatureCalculator heapAllocRateTotal =
                new HeapAllocRateTotalTemperatureCalculator();

        CpuUtilByShardsMetricBasedTemperatureCalculator cpuUtilByShard =
                new CpuUtilByShardsMetricBasedTemperatureCalculator();
        AvgCpuUtilByShardsMetricBasedTemperatureCalculator avgCpuUtilByShards =
                new AvgCpuUtilByShardsMetricBasedTemperatureCalculator();
        ShardIndependentTemperatureCalculatorCpuUtilMetric shardIndependentCpuUtilMetric =
                new ShardIndependentTemperatureCalculatorCpuUtilMetric();
        TotalCpuUtilForTotalNodeMetric cpuUtilPeakUsage = new TotalCpuUtilForTotalNodeMetric();

        ShardSizeMetricBasedTemperatureCalculator shardSizeByShard =
                new ShardSizeMetricBasedTemperatureCalculator();
        ShardSizeAvgTemperatureCalculator shardSizeAvg = new ShardSizeAvgTemperatureCalculator();
        ShardTotalDiskUsageTemperatureCalculator shardTotalDiskUsage =
                new ShardTotalDiskUsageTemperatureCalculator();

        // heat map is developed only for data nodes.
        cpuUtilByShard.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        avgCpuUtilByShards.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        shardIndependentCpuUtilMetric.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        cpuUtilPeakUsage.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);

        heapAllocByShard.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        heapAllocRateByShardAvg.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        shardIndependentHeapAllocRate.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        heapAllocRateTotal.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);

        shardSizeByShard.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        shardSizeAvg.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        shardTotalDiskUsage.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);

        addLeaf(cpuUtilByShard);
        addLeaf(avgCpuUtilByShards);
        addLeaf(shardIndependentCpuUtilMetric);
        addLeaf(cpuUtilPeakUsage);

        addLeaf(heapAllocByShard);
        addLeaf(heapAllocRateByShardAvg);
        addLeaf(shardIndependentHeapAllocRate);
        addLeaf(heapAllocRateTotal);

        addLeaf(shardSizeByShard);
        addLeaf(shardSizeAvg);
        addLeaf(shardTotalDiskUsage);

        CpuUtilDimensionTemperatureRca cpuUtilHeat =
                new CpuUtilDimensionTemperatureRca(
                        EVALUATION_INTERVAL_SECONDS,
                        shardStore,
                        cpuUtilByShard,
                        avgCpuUtilByShards,
                        shardIndependentCpuUtilMetric,
                        cpuUtilPeakUsage);
        cpuUtilHeat.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        cpuUtilHeat.addAllUpstreams(
                Arrays.asList(
                        cpuUtilByShard,
                        avgCpuUtilByShards,
                        shardIndependentCpuUtilMetric,
                        cpuUtilPeakUsage));

        HeapAllocRateTemperatureRca heapAllocRateHeat =
                new HeapAllocRateTemperatureRca(
                        EVALUATION_INTERVAL_SECONDS,
                        shardStore,
                        heapAllocByShard,
                        heapAllocRateByShardAvg,
                        shardIndependentHeapAllocRate,
                        heapAllocRateTotal);

        heapAllocRateHeat.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        heapAllocRateHeat.addAllUpstreams(
                Arrays.asList(
                        heapAllocByShard,
                        heapAllocRateByShardAvg,
                        shardIndependentHeapAllocRate,
                        heapAllocRateTotal));

        ShardSizeDimensionTemperatureRca shardSizeHeat =
                new ShardSizeDimensionTemperatureRca(
                        EVALUATION_INTERVAL_SECONDS,
                        shardStore,
                        shardSizeByShard,
                        shardSizeAvg,
                        shardTotalDiskUsage);
        shardSizeHeat.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        shardSizeHeat.addAllUpstreams(
                Arrays.asList(shardSizeByShard, shardSizeAvg, shardTotalDiskUsage));

        NodeTemperatureRca nodeTemperatureRca =
                new NodeTemperatureRca(cpuUtilHeat, heapAllocRateHeat, shardSizeHeat);
        nodeTemperatureRca.addTag(
                RcaConsts.RcaTagConstants.TAG_LOCUS,
                RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
        nodeTemperatureRca.addAllUpstreams(
                Arrays.asList(cpuUtilHeat, heapAllocRateHeat, shardSizeHeat));

        //    ClusterTemperatureRca clusterTemperatureRca = new
        // ClusterTemperatureRca(nodeTemperatureRca);
        //    clusterTemperatureRca.addTag(TAG_LOCUS, LOCUS_CLUSTER_MANAGER_NODE);
        //    clusterTemperatureRca.addTag(TAG_AGGREGATE_UPSTREAM, LOCUS_DATA_NODE);
        //    clusterTemperatureRca.addAllUpstreams(Collections.singletonList(nodeTemperatureRca));
    }
}
