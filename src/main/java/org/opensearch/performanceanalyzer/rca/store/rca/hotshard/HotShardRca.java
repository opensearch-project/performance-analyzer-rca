/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.hotshard;


import com.google.common.collect.MinMaxPriorityQueue;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Record;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.OSMetrics;
import org.opensearch.performanceanalyzer.commons.stats.ServiceMetrics;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.grpc.HotShardSummaryMessage.CriteriaEnum;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.configs.HotShardRcaConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.SummarizedWindow;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotShardSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaVerticesMetrics;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;

/**
 * This RCA is to identify a hot shard within an index. A Hot shard is an outlier within its
 * counterparts. The RCA subscribes to following metrics : 1. CPU_Utilization 2. Heap_AllocRate.
 *
 * <p>The RCA looks at the above 2 metric data, compares the values against the threshold for each
 * resource and if the usage for any of 2 resources is greater than their individual threshold, we
 * mark the context as 'UnHealthy' and create a HotShardResourceSummary for the shard.
 *
 * <p>This RCA is to be used as an upstream Node to the {@link HotShardClusterRca}.
 *
 * <p>Optional metrics which can be added in the future : 1. IO_TotThroughput 2. Thread_Blocked_Time
 */
public class HotShardRca extends Rca<ResourceFlowUnit<HotNodeSummary>> {

    private static final Logger LOG = LogManager.getLogger(HotShardRca.class);
    private static final int SLIDING_WINDOW_IN_SECONDS = 60;

    private double cpuUtilizationThreshold;
    private double heapAllocRateThreshold;
    private int topKConsumers;
    private final Metric cpuUtilization;
    private final Metric heapAllocRate;
    private final int rcaPeriod;
    private int counter;
    protected Clock clock;
    /* HashMap with IndexShardKey object as key and SummarizedWindow object of metric data as value
    which contains both metrics of interest and their common timestamps*/
    private Map<IndexShardKey, SummarizedWindow> shardResourceSummarizationMap;

    public <M extends Metric> HotShardRca(
            final long evaluationIntervalSeconds,
            final int rcaPeriod,
            final M cpuUtilization,
            final M heapAllocRate) {
        super(evaluationIntervalSeconds);
        this.cpuUtilization = cpuUtilization;
        this.heapAllocRate = heapAllocRate;
        this.rcaPeriod = rcaPeriod;
        this.counter = 0;
        this.clock = Clock.systemUTC();
        this.shardResourceSummarizationMap = new HashMap<>();
        this.cpuUtilizationThreshold = HotShardRcaConfig.DEFAULT_CPU_UTILIZATION_THRESHOLD;
        this.heapAllocRateThreshold =
                HotShardRcaConfig.DEFAULT_HEAP_ALLOC_RATE_THRESHOLD_IN_BYTE_PER_SEC;
        this.topKConsumers = HotShardRcaConfig.DEFAULT_TOP_K_CONSUMERS;
    }

    private void consumeFlowUnit(final MetricFlowUnit metricFlowUnit, OSMetrics metricType) {
        for (Record record : metricFlowUnit.getData()) {
            try {
                String indexName =
                        record.getValue(
                                AllMetrics.CommonDimension.INDEX_NAME.toString(), String.class);
                Integer shardId =
                        record.getValue(
                                AllMetrics.CommonDimension.SHARD_ID.toString(), Integer.class);
                if (indexName != null && shardId != null) {
                    IndexShardKey indexShardKey = IndexShardKey.buildIndexShardKey(record);
                    double usage = record.getValue(MetricsDB.SUM, Double.class);
                    SummarizedWindow usageWindow = shardResourceSummarizationMap.get(indexShardKey);
                    if (null == usageWindow) {
                        usageWindow = new SummarizedWindow();
                        shardResourceSummarizationMap.put(indexShardKey, usageWindow);
                    }
                    usageWindow.next(metricType, usage, this.clock.millis());
                }
            } catch (Exception e) {
                ServiceMetrics.RCA_VERTICES_METRICS_AGGREGATOR.updateStat(
                        RcaVerticesMetrics.HOT_SHARD_RCA_ERROR, 1);
                LOG.error(
                        "Failed to parse metric in FlowUnit: {} from {}",
                        record,
                        metricType.toString(),
                        e);
            }
        }
    }

    private void consumeMetric(Metric metric, OSMetrics metricType) {
        for (MetricFlowUnit metricFlowUnit : metric.getFlowUnits()) {
            if (metricFlowUnit.getData() != null) {
                consumeFlowUnit(metricFlowUnit, metricType);
            }
        }
    }

    /**
     * If Shard is already meant to be sent to {@link HotShardClusterRca} we just mark his other
     * criteria, by setting the field to double.
     *
     * <p>If it's shard's first occurrence, we fill both the metric values and set adequate
     * criteria.
     */
    private void drainQueue(
            MinMaxPriorityQueue<NamedSummarizedWindow> queue,
            Map<IndexShardKey, HotShardSummary> consumersToSend,
            InstanceDetails instanceDetails,
            OSMetrics metricType) {
        while (!queue.isEmpty()) {
            NamedSummarizedWindow candidate = queue.remove();
            if (consumersToSend.containsKey(candidate.indexShardKey)) {
                HotShardSummary summary = consumersToSend.get(candidate.indexShardKey);
                summary.setCriteria(CriteriaEnum.DOUBLE_CRITERIA);
                continue;
            }
            HotShardSummary summary =
                    new HotShardSummary(
                            candidate.indexShardKey.getIndexName(),
                            String.valueOf(candidate.indexShardKey.getShardId()),
                            instanceDetails.getInstanceId().toString(),
                            SLIDING_WINDOW_IN_SECONDS);

            double avgCpuUtilization =
                    candidate.summarizedWindow.readAvgCpuUtilization(TimeUnit.SECONDS);
            double avgHeapAllocRate =
                    candidate.summarizedWindow.readAvgHeapAllocRate(TimeUnit.SECONDS);

            summary.setCpuUtilization(avgCpuUtilization);
            summary.setHeapAllocRate(avgHeapAllocRate);
            summary.setCriteria(
                    OSMetrics.CPU_UTILIZATION.equals(metricType)
                            ? CriteriaEnum.CPU_UTILIZATION_CRITERIA
                            : CriteriaEnum.HEAP_ALLOC_RATE_CRITERIA);

            consumersToSend.put(candidate.indexShardKey, summary);
        }
    }

    private void isTopConsumer(
            MinMaxPriorityQueue<NamedSummarizedWindow> queue,
            IndexShardKey indexShardKey,
            SummarizedWindow summarizedWindow,
            OSMetrics metricType,
            double threshold) {
        double metricValue = summarizedWindow.readAvgMetricValue(TimeUnit.SECONDS, metricType);
        if (metricValue > threshold) {
            queue.add(new NamedSummarizedWindow(summarizedWindow, indexShardKey));

            LOG.debug(
                    "Top consumer Identified, Shard : {} , metricValue = {} , metricThreshold = {}, metricType = {}",
                    indexShardKey.toString(),
                    metricValue,
                    threshold,
                    metricType.toString());
        }
    }

    /**
     * Locally identifies hot shards on the node. The function uses CPU_Utilization, HEAP_Alloc_Rate
     * FlowUnits to identify a Hot Shard.
     *
     * <p>We specify the threshold for CPU_Utilization, HEAP_Alloc_Rate and any shard using either
     * of 2 resources more than the specified threshold is declared top consumer.
     */
    @Override
    public ResourceFlowUnit<HotNodeSummary> operate() {
        counter += 1;
        // Populate the Resource maps
        consumeMetric(cpuUtilization, OSMetrics.CPU_UTILIZATION);
        consumeMetric(heapAllocRate, OSMetrics.HEAP_ALLOC_RATE);

        if (counter == rcaPeriod) {
            /* We limit the queues by maxConsumersToSend. This guarantees no heap re-allocations for
            underlying structures plus we ensure constant memory complexity for the duration of the function */
            MinMaxPriorityQueue<NamedSummarizedWindow> cpuUtilTopConsumers =
                    MinMaxPriorityQueue.orderedBy(new SummarizedWindowCPUComparator())
                            .maximumSize(topKConsumers)
                            .create();

            MinMaxPriorityQueue<NamedSummarizedWindow> heapAllocRateTopConsumers =
                    MinMaxPriorityQueue.orderedBy(new SummarizedWindowHEAPComparator())
                            .maximumSize(topKConsumers)
                            .create();

            for (Map.Entry<IndexShardKey, SummarizedWindow> entry :
                    shardResourceSummarizationMap.entrySet()) {
                // Shard can end up in both queues, which will share the reference of the same
                // window object
                isTopConsumer(
                        cpuUtilTopConsumers,
                        entry.getKey(),
                        entry.getValue(),
                        OSMetrics.CPU_UTILIZATION,
                        cpuUtilizationThreshold);

                isTopConsumer(
                        heapAllocRateTopConsumers,
                        entry.getKey(),
                        entry.getValue(),
                        OSMetrics.HEAP_ALLOC_RATE,
                        heapAllocRateThreshold);
            }

            shardResourceSummarizationMap.clear();

            InstanceDetails instanceDetails = getInstanceDetails();
            Map<IndexShardKey, HotShardSummary> consumersToSend = new HashMap<>();

            drainQueue(
                    cpuUtilTopConsumers,
                    consumersToSend,
                    instanceDetails,
                    OSMetrics.CPU_UTILIZATION);
            drainQueue(
                    heapAllocRateTopConsumers,
                    consumersToSend,
                    instanceDetails,
                    OSMetrics.HEAP_ALLOC_RATE);

            HotNodeSummary nodeSummary =
                    new HotNodeSummary(
                            instanceDetails.getInstanceId(), instanceDetails.getInstanceIp());

            nodeSummary.setHotShardSummaryList(new ArrayList<>(consumersToSend.values()));

            ResourceContext context =
                    new ResourceContext(
                            nodeSummary.getNestedSummaryList().isEmpty()
                                    ? Resources.State.HEALTHY
                                    : Resources.State.UNHEALTHY);
            // reset the variables
            counter = 0;
            // check if the current node is data node. If it is the data node
            // then HotNodeRca is the top level RCA on this node, and we want to persist summaries
            // in
            // FlowUnit.
            boolean isDataNode = !instanceDetails.getIsClusterManager();
            return new ResourceFlowUnit<>(this.clock.millis(), context, nodeSummary, isDataNode);
        } else {
            LOG.debug("Empty FlowUnit returned for Hot Shard RCA");
            return new ResourceFlowUnit<>(this.clock.millis());
        }
    }
    /**
     * read threshold values from rca.conf
     *
     * @param conf RcaConf object
     */
    @Override
    public void readRcaConf(RcaConf conf) {
        HotShardRcaConfig configObj = conf.getHotShardRcaConfig();
        cpuUtilizationThreshold = configObj.getCpuUtilizationThreshold();
        heapAllocRateThreshold = configObj.getHeapAllocRateThreshold();
        topKConsumers = configObj.getMaximumConsumersToSend();
    }

    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        final List<FlowUnitMessage> flowUnitMessages =
                args.getWireHopper().readFromWire(args.getNode());
        List<ResourceFlowUnit<HotNodeSummary>> flowUnitList = new ArrayList<>();
        LOG.debug("rca: Executing fromWire: {}", this.getClass().getSimpleName());
        for (FlowUnitMessage flowUnitMessage : flowUnitMessages) {
            flowUnitList.add(ResourceFlowUnit.buildFlowUnitFromWrapper(flowUnitMessage));
        }
        setFlowUnits(flowUnitList);
    }
}

/* When going into queues from maps, Shard identification (from map key) has to be persisted.
This class is used for IndexShardKey reconstruction from priority queues */
class NamedSummarizedWindow {
    protected SummarizedWindow summarizedWindow;
    protected IndexShardKey indexShardKey;

    public NamedSummarizedWindow(SummarizedWindow summarizedWindow, IndexShardKey indexShardKey) {
        this.summarizedWindow = summarizedWindow;
        this.indexShardKey = indexShardKey;
    }
}
/* Comparators for SummarizedWindow, comparing by different metrics.
This way already existing structures can be recycled. */
class SummarizedWindowCPUComparator implements Comparator<NamedSummarizedWindow> {
    @Override
    public int compare(NamedSummarizedWindow o1, NamedSummarizedWindow o2) {
        return Double.compare(
                o2.summarizedWindow.readAvgCpuUtilization(TimeUnit.SECONDS),
                o1.summarizedWindow.readAvgCpuUtilization(TimeUnit.SECONDS));
    }
}

class SummarizedWindowHEAPComparator implements Comparator<NamedSummarizedWindow> {
    @Override
    public int compare(NamedSummarizedWindow o1, NamedSummarizedWindow o2) {
        return Double.compare(
                o2.summarizedWindow.readAvgHeapAllocRate(TimeUnit.SECONDS),
                o1.summarizedWindow.readAvgHeapAllocRate(TimeUnit.SECONDS));
    }
}
