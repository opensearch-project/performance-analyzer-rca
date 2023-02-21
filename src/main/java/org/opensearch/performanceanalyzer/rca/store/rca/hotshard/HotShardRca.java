/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.hotshard;


import com.google.common.collect.MinMaxPriorityQueue;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Record;
import org.openjdk.jol.info.GraphLayout;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
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
 * counterparts. The RCA subscribes to following metrics : 1. CPU_Utilization 2. IO_TotThroughput 3.
 * IO_TotalSyscallRate
 *
 * <p>The RCA looks at the above 3 metric data, compares the values against the threshold for each
 * resource and if the usage for any of 3 resources is greater than their individual threshold, we
 * mark the context as 'UnHealthy' and create a HotShardResourceSummary for the shard.
 *
 * <p>Optional metrics which can be added in future : 1. Heap_AllocRate 2. Paging_RSS
 */
public class HotShardRca extends Rca<ResourceFlowUnit<HotNodeSummary>> {

    private static final Logger LOG = LogManager.getLogger(HotShardRca.class);
    private static final int SLIDING_WINDOW_IN_SECONDS = 60;

    private double cpuUtilizationThreshold;
    private double heapAllocRateThreshold;
    private int maxConsumersToSend;
    private final Metric cpuUtilization;
    private final Metric heapAllocRate;
    private final int rcaPeriod;
    private int counter;
    protected Clock clock;

    // HashMap with IndexShardKey object as key and SlidingWindowData object of metric data as value
    private Map<IndexShardKey, SummarizedWindow> summarizationMap;

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
        this.summarizationMap = new HashMap<>();
        this.cpuUtilizationThreshold = HotShardRcaConfig.DEFAULT_CPU_UTILIZATION_THRESHOLD;
        this.heapAllocRateThreshold =
                HotShardRcaConfig.DEFAULT_HEAP_ALLOC_RATE_THRESHOLD_IN_BYTE_PER_SEC;
        this.maxConsumersToSend = HotShardRcaConfig.DEFAULT_MAXIMUM_CONSUMERS_TO_SEND;
    }

    private void consumeFlowUnit(
            final MetricFlowUnit metricFlowUnit, AllMetrics.OSMetrics metricType) {
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

                    SummarizedWindow usageWindow = summarizationMap.get(indexShardKey);
                    if (null == usageWindow) {
                        usageWindow = new SummarizedWindow();
                        summarizationMap.put(indexShardKey, usageWindow);
                    }

                    usageWindow.next(metricType, usage, this.clock.millis());
                }
            } catch (Exception e) {
                PerformanceAnalyzerApp.RCA_VERTICES_METRICS_AGGREGATOR.updateStat(
                        RcaVerticesMetrics.HOT_SHARD_RCA_ERROR, "", 1);
                LOG.error(
                        "Failed to parse metric in FlowUnit: {} from {}",
                        record,
                        metricType.toString(),
                        e);
            }
        }
    }

    private void consumeMetric(Metric metric, AllMetrics.OSMetrics metricType) {
        for (MetricFlowUnit metricFlowUnit : metric.getFlowUnits()) {
            if (metricFlowUnit.getData() != null) {
                consumeFlowUnit(metricFlowUnit, metricType);
            }
        }
    }

    private void checkAgainstThreshold(
            MinMaxPriorityQueue<NamedSummarizedWindow> queue,
            Map.Entry<IndexShardKey, SummarizedWindow> entry,
            AllMetrics.OSMetrics metricType,
            double threshold) {
        // TODO : Check if null can break out here
        double metricValue = entry.getValue().readAvgMetricValue(TimeUnit.SECONDS, metricType);
        if (metricValue > threshold) {
            queue.add(new NamedSummarizedWindow(entry.getValue(), entry.getKey()));

            LOG.debug(
                    "Hot Shard Identified, Shard : {} , metricValue = {} , metricThreshold = {}, metricType = {}",
                    entry.getKey().toString(),
                    metricValue,
                    threshold,
                    metricType.toString());
        }
    }

    private void drainQueue(
            MinMaxPriorityQueue<NamedSummarizedWindow> queue,
            Map<IndexShardKey, SummarizedWindow> consumersToSend) {
        while (!queue.isEmpty()) {
            NamedSummarizedWindow candidate = queue.remove();
            if (consumersToSend.containsKey(candidate.indexShardKey)) {
                continue;
            }
            consumersToSend.put(candidate.indexShardKey, candidate.summarizedWindow);
        }
    }

    private HotNodeSummary summarize(
            Map<IndexShardKey, SummarizedWindow> consumersToSend, InstanceDetails instanceDetails) {

        HotNodeSummary nodeSummary =
                new HotNodeSummary(
                        instanceDetails.getInstanceId(), instanceDetails.getInstanceIp());

        for (Map.Entry<IndexShardKey, SummarizedWindow> entry : consumersToSend.entrySet()) {

            IndexShardKey indexShardKey = entry.getKey();
            double avgCpuUtilization = entry.getValue().readAvgCpuUtilization(TimeUnit.SECONDS);
            double avgHeapAllocRate = entry.getValue().readAvgHeapAllocRate(TimeUnit.SECONDS);

            HotShardSummary summary =
                    new HotShardSummary(
                            indexShardKey.getIndexName(),
                            String.valueOf(indexShardKey.getShardId()),
                            instanceDetails.getInstanceId().toString(),
                            SLIDING_WINDOW_IN_SECONDS);

            summary.setcpuUtilization(avgCpuUtilization);
            summary.setCpuUtilizationThreshold(cpuUtilizationThreshold);
            summary.setHeapAllocRate(avgHeapAllocRate);
            summary.setHeapAllocRateThreshold(heapAllocRateThreshold);

            nodeSummary.appendNestedSummary(summary);
        }

        return nodeSummary;
    }

    /**
     * Locally identifies hot shards on the node. The function uses CPU_Utilization, HEAP_Alloc_Rate
     * FlowUnits to identify a Hot Shard.
     *
     * <p>We specify the threshold for CPU_Utilization, HEAP_Alloc_Rate and any shard using either
     * of 3 resources more than the specified threshold is declared Hot.
     */
    @Override
    public ResourceFlowUnit<HotNodeSummary> operate() {
        counter += 1;
        // Populate the Resource maps
        consumeMetric(cpuUtilization, AllMetrics.OSMetrics.CPU_UTILIZATION);
        consumeMetric(heapAllocRate, AllMetrics.OSMetrics.HEAP_ALLOC_RATE);

        if (counter == rcaPeriod) {
            /* We limit the queues by maxConsumersToSend. This guarantees no re-allocations
            plus we ensure constant memory complexity for the duration of the function */
            MinMaxPriorityQueue<NamedSummarizedWindow> cpuUtilConsumers =
                    MinMaxPriorityQueue.orderedBy(new SummarizedWindowCPUComparator())
                            .maximumSize(maxConsumersToSend)
                            .create();

            MinMaxPriorityQueue<NamedSummarizedWindow> heapAllocConsumers =
                    MinMaxPriorityQueue.orderedBy(new SummarizedWindowHEAPComparator())
                            .maximumSize(maxConsumersToSend)
                            .create();

            for (Map.Entry<IndexShardKey, SummarizedWindow> entry : summarizationMap.entrySet()) {
                checkAgainstThreshold(
                        cpuUtilConsumers,
                        entry,
                        AllMetrics.OSMetrics.CPU_UTILIZATION,
                        cpuUtilizationThreshold);

                checkAgainstThreshold(
                        heapAllocConsumers,
                        entry,
                        AllMetrics.OSMetrics.HEAP_ALLOC_RATE,
                        heapAllocRateThreshold);
            }

            summarizationMap.clear();

            Map<IndexShardKey, SummarizedWindow> consumersToSend = new HashMap<>();
            drainQueue(cpuUtilConsumers, consumersToSend);
            drainQueue(heapAllocConsumers, consumersToSend);

            InstanceDetails instanceDetails = getInstanceDetails();
            HotNodeSummary nodeSummary = summarize(consumersToSend, instanceDetails);
            ResourceContext context =
                    new ResourceContext(
                            nodeSummary.getNestedSummaryList().isEmpty()
                                    ? Resources.State.HEALTHY
                                    : Resources.State.UNHEALTHY);
            // reset the variables
            counter = 0;
            // check if the current node is data node. If it is the data node
            // then HotNodeRca is the top level RCA on this node and we want to persist summaries in
            // flowunit.
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
        maxConsumersToSend = configObj.getMaximumConsumersToSend();
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

/* This class is used for IndexShardKey reconstruction from priority queues */
class NamedSummarizedWindow {
    protected SummarizedWindow summarizedWindow;
    protected IndexShardKey indexShardKey;

    public NamedSummarizedWindow(SummarizedWindow summarizedWindow, IndexShardKey indexShardKey) {
        this.summarizedWindow = summarizedWindow;
        this.indexShardKey = indexShardKey;
    }
}

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
