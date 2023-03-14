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
    private int topKConsumers;
    private final Metric cpuUtilization;
    private final Metric heapAllocRate;
    private final int rcaPeriod;
    private int counter;
    protected Clock clock;
    /* HashMap with IndexShardKey object as key and SummarizedWindow object of metric data as value
       which contains both metrics of interest and their common timestamps
    */
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

    private double getResourceThreshold(AllMetrics.OSMetrics metricType) {
        if (AllMetrics.OSMetrics.HEAP_ALLOC_RATE.equals(metricType)) {
            return heapAllocRateThreshold;
        } else {
            return cpuUtilizationThreshold;
        }
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
                    SummarizedWindow usageWindow = shardResourceSummarizationMap.get(indexShardKey);
                    if (null == usageWindow) {
                        usageWindow = new SummarizedWindow();
                        shardResourceSummarizationMap.put(indexShardKey, usageWindow);
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

    private void isTopConsumer(
            MinMaxPriorityQueue<NamedSummarizedWindow> queue,
            IndexShardKey indexShardKey,
            SummarizedWindow summarizedWindow,
            AllMetrics.OSMetrics metricType,
            double threshold) {
        // TODO : Check if null can break out here
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

    private void summarize(
            HotNodeSummary nodeSummary,
            MinMaxPriorityQueue<NamedSummarizedWindow> queue,
            InstanceDetails instanceDetails,
            AllMetrics.OSMetrics metricType) {

        while (!queue.isEmpty()) {
            NamedSummarizedWindow candidate = queue.remove();
            IndexShardKey indexShardKey = candidate.indexShardKey;
            double avgResourceValue =
                    candidate.summarizedWindow.readAvgMetricValue(TimeUnit.SECONDS, metricType);
            double resourceThreshold = getResourceThreshold(metricType);

            HotShardSummary summary =
                    new HotShardSummary(
                            indexShardKey.getIndexName(),
                            String.valueOf(indexShardKey.getShardId()),
                            instanceDetails.getInstanceId().toString(),
                            SLIDING_WINDOW_IN_SECONDS);

            LOG.error(
                    "Sending "
                            + indexShardKey
                            + " with metric: "
                            + avgResourceValue
                            + " ["
                            + metricType
                            + "].");

            summary.setResourceValue(avgResourceValue);
            summary.setResourceThreshold(resourceThreshold);
            summary.setResource(metricType.toString());

            nodeSummary.appendNestedSummary(summary);
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
        consumeMetric(cpuUtilization, AllMetrics.OSMetrics.CPU_UTILIZATION);
        consumeMetric(heapAllocRate, AllMetrics.OSMetrics.HEAP_ALLOC_RATE);

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
                isTopConsumer(
                        cpuUtilTopConsumers,
                        entry.getKey(),
                        entry.getValue(),
                        AllMetrics.OSMetrics.CPU_UTILIZATION,
                        cpuUtilizationThreshold);

                isTopConsumer(
                        heapAllocRateTopConsumers,
                        entry.getKey(),
                        entry.getValue(),
                        AllMetrics.OSMetrics.HEAP_ALLOC_RATE,
                        heapAllocRateThreshold);

                LOG.error(
                        "Encountered "
                                + entry.getKey()
                                + " with metrics: ("
                                + entry.getValue().readAvgCpuUtilization(TimeUnit.SECONDS)
                                + ", "
                                + entry.getValue().readAvgHeapAllocRate(TimeUnit.SECONDS)
                                + ")");
            }

            shardResourceSummarizationMap.clear();

            LOG.error("SEND_START");

            InstanceDetails instanceDetails = getInstanceDetails();
            HotNodeSummary nodeSummary =
                    new HotNodeSummary(
                            instanceDetails.getInstanceId(), instanceDetails.getInstanceIp());
            LOG.error("SEND_START");
            summarize(
                    nodeSummary,
                    heapAllocRateTopConsumers,
                    instanceDetails,
                    AllMetrics.OSMetrics.HEAP_ALLOC_RATE);
            summarize(
                    nodeSummary,
                    cpuUtilTopConsumers,
                    instanceDetails,
                    AllMetrics.OSMetrics.CPU_UTILIZATION);
            LOG.error("SEND_END");
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
