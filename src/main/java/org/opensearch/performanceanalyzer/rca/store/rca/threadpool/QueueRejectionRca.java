/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 * Copyright 2019-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.opensearch.performanceanalyzer.rca.store.rca.threadpool;


import com.google.common.annotations.VisibleForTesting;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.configs.QueueRejectionRcaConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.persist.SQLParsingUtil;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaGraphMetrics;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;

/**
 * This RCA reads ThreadPool_RejectionReqs from upstream metrics and maintain collectors for each
 * thread pool queue type(currently we only support write/search queue). Each collector keeps track
 * of the time window period(tp) where we repeatedly see rejections for the last tp duration. This
 * RCA is marked as unhealthy if tp we find tp is above the threshold(300 seconds).
 */
public class QueueRejectionRca extends Rca<ResourceFlowUnit<HotNodeSummary>> {
    private static final Logger LOG = LogManager.getLogger(QueueRejectionRca.class);
    private final int rcaPeriod;
    private final List<QueueRejectionCollector> queueRejectionCollectors;
    private int counter;
    protected Clock clock;

    public <M extends Metric> QueueRejectionRca(final int rcaPeriod, M threadPool_RejectedReqs) {
        super(5);
        this.rcaPeriod = rcaPeriod;
        counter = 0;
        clock = Clock.systemUTC();
        queueRejectionCollectors =
                Collections.unmodifiableList(
                        Arrays.asList(
                                new QueueRejectionCollector(
                                        ResourceUtil.WRITE_QUEUE_REJECTION,
                                        AllMetrics.ThreadPoolType.WRITE,
                                        threadPool_RejectedReqs),
                                new QueueRejectionCollector(
                                        ResourceUtil.SEARCH_QUEUE_REJECTION,
                                        AllMetrics.ThreadPoolType.SEARCH,
                                        threadPool_RejectedReqs)));
    }

    @VisibleForTesting
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public ResourceFlowUnit<HotNodeSummary> operate() {
        counter += 1;
        long currTimestamp = clock.millis();
        // collect rejection metrics
        for (QueueRejectionCollector collector : queueRejectionCollectors) {
            collector.collect(currTimestamp);
        }
        if (counter == rcaPeriod) {
            counter = 0;
            InstanceDetails instanceDetails = getInstanceDetails();
            HotNodeSummary nodeSummary =
                    new HotNodeSummary(
                            instanceDetails.getInstanceId(), instanceDetails.getInstanceIp());
            boolean hasUnhealthyQueue = false;
            for (QueueRejectionCollector collector : queueRejectionCollectors) {
                // if we've see thread pool rejection in the last 5 mins, the thread pool is
                // considered as contended
                if (collector.isUnhealthy(currTimestamp)) {
                    nodeSummary.appendNestedSummary(collector.generateSummary(currTimestamp));
                    hasUnhealthyQueue = true;
                }
            }
            ResourceContext context;
            if (!hasUnhealthyQueue) {
                context = new ResourceContext(Resources.State.HEALTHY);
            } else {
                context = new ResourceContext(Resources.State.UNHEALTHY);
            }
            boolean isDataNode = !instanceDetails.getIsMaster();
            return new ResourceFlowUnit<>(currTimestamp, context, nodeSummary, isDataNode);
        } else {
            return new ResourceFlowUnit<>(currTimestamp);
        }
    }

    // TODO: move this method back into the Rca base class
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

    /**
     * read rejection-time-period-in-seconds from rca.conf
     *
     * @param conf RcaConf object
     */
    @Override
    public void readRcaConf(RcaConf conf) {
        QueueRejectionRcaConfig configObj = conf.getQueueRejectionRcaConfig();
        long rejectedTimePeriod =
                TimeUnit.SECONDS.toMillis(configObj.getRejectionTimePeriodInSeconds());
        queueRejectionCollectors.forEach(
                collector -> collector.setRejectionTimePeriod(rejectedTimePeriod));
    }

    /** A collector class to collect rejection from each queue type */
    private static class QueueRejectionCollector {
        private final Resource threadPool;
        private final AllMetrics.ThreadPoolType threadPoolMetric;
        private final Metric threadPool_RejectedReqs;
        private boolean hasRejection;
        private long rejectionTimestamp;
        private long rejectionTimePeriodInMillis;
        private int clearCounter;
        private int consecutivePeriodsToClear;

        public QueueRejectionCollector(
                final Resource threadPool,
                final AllMetrics.ThreadPoolType threadPoolMetric,
                final Metric threadPool_RejectedReqs) {
            this.threadPool = threadPool;
            this.threadPoolMetric = threadPoolMetric;
            this.threadPool_RejectedReqs = threadPool_RejectedReqs;
            this.hasRejection = false;
            this.rejectionTimestamp = 0;
            this.rejectionTimePeriodInMillis =
                    TimeUnit.SECONDS.toMillis(
                            QueueRejectionRcaConfig.DEFAULT_REJECTION_TIME_PERIOD_IN_SECONDS);
            this.clearCounter = 0;
            this.consecutivePeriodsToClear = 3;
        }

        public void setRejectionTimePeriod(long rejectionTimePeriodInMillis) {
            this.rejectionTimePeriodInMillis = rejectionTimePeriodInMillis;
        }

        public void collect(final long currTimestamp) {
            for (MetricFlowUnit flowUnit : threadPool_RejectedReqs.getFlowUnits()) {
                if (flowUnit.isEmpty()) {
                    clearCounter += 1;
                    if (clearCounter > consecutivePeriodsToClear) {
                        // If the RCA receives 3 empty flow units, re-set the 'hasMetric' value
                        hasRejection = false;
                        clearCounter = 0;
                        // This is expected on master nodes.
                        LOG.debug(
                                "{} encountered {} empty flow units, re-setting the hasRejection value.",
                                this.getClass().getSimpleName(),
                                consecutivePeriodsToClear);
                    }
                    PerformanceAnalyzerApp.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                            RcaGraphMetrics.RCA_RX_EMPTY_FU, this.getClass().getSimpleName(), 1);
                    continue;
                }
                double rejectCnt =
                        SQLParsingUtil.readDataFromSqlResult(
                                flowUnit.getData(),
                                AllMetrics.ThreadPoolDimension.THREAD_POOL_TYPE.getField(),
                                threadPoolMetric.toString(),
                                MetricsDB.MAX);
                if (!Double.isNaN(rejectCnt)) {
                    if (rejectCnt > 0) {
                        if (!hasRejection) {
                            rejectionTimestamp = currTimestamp;
                        }
                        hasRejection = true;
                    } else {
                        hasRejection = false;
                    }
                } else {
                    LOG.error("Failed to parse metric from threadpool {}", threadPool.toString());
                }
            }
        }

        public boolean isUnhealthy(final long currTimestamp) {
            return hasRejection
                    && (currTimestamp - rejectionTimestamp) >= rejectionTimePeriodInMillis;
        }

        public HotResourceSummary generateSummary(final long currTimestamp) {
            HotResourceSummary resourceSummary = null;
            if (isUnhealthy(currTimestamp)) {
                resourceSummary =
                        new HotResourceSummary(
                                threadPool,
                                TimeUnit.MILLISECONDS.toSeconds(rejectionTimePeriodInMillis),
                                TimeUnit.MILLISECONDS.toSeconds(currTimestamp - rejectionTimestamp),
                                0);
            }
            return resourceSummary;
        }
    }
}
