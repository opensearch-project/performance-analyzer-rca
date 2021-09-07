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
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package org.opensearch.performanceanalyzer.rca.store.rca.admissioncontrol;

import static org.opensearch.performanceanalyzer.PerformanceAnalyzerApp.RCA_VERTICES_METRICS_AGGREGATOR;
import static org.opensearch.performanceanalyzer.metrics.AllMetrics.GCType.HEAP;
import static org.opensearch.performanceanalyzer.metrics.AllMetrics.HeapDimension.MEM_TYPE;
import static org.opensearch.performanceanalyzer.rca.framework.api.Resources.State.UNHEALTHY;
import static org.opensearch.performanceanalyzer.rca.framework.api.persist.SQLParsingUtil.readDataFromSqlResult;
import static org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil.HEAP_MAX_SIZE;
import static org.opensearch.performanceanalyzer.rca.framework.metrics.RcaVerticesMetrics.ADMISSION_CONTROL_RCA_TRIGGERED;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Field;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.configs.AdmissionControlRcaConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;
import org.opensearch.performanceanalyzer.util.range.Range;
import org.opensearch.performanceanalyzer.util.range.RangeConfiguration;
import org.opensearch.performanceanalyzer.util.range.RequestSizeHeapRangeConfiguration;

public class AdmissionControlRca extends Rca<ResourceFlowUnit<HotNodeSummary>> {
    private static final Logger LOG = LogManager.getLogger(AdmissionControlRca.class);
    private static final double BYTES_TO_MEGABYTES = Math.pow(1024, 2);

    // Global JVM Memory Pressure Metric
    public static final String GLOBAL_JVMMP = "Global_JVMMP";
    // Request Size Metric
    public static final String REQUEST_SIZE = "Request_Size";

    private final Metric heapUsedValue;
    private final Metric heapMaxValue;

    private final RangeConfiguration requestSizeHeapRange;
    private final int rcaPeriod;
    private int counter;
    private double previousHeapPercent;

    public <M extends Metric> AdmissionControlRca(
            final int rcaPeriodInSeconds, final M heapUsedValue, final M heapMaxValue) {
        super(rcaPeriodInSeconds);
        this.counter = 0;
        this.previousHeapPercent = 0.0;
        this.rcaPeriod = rcaPeriodInSeconds;
        this.heapUsedValue = heapUsedValue;
        this.heapMaxValue = heapMaxValue;
        this.requestSizeHeapRange = new RequestSizeHeapRangeConfiguration();
    }

    private <M extends Metric> double getMetric(
            M metric, Field<String> field, String fieldName, String dataField) {
        AtomicReference<Double> metricValue = new AtomicReference<>((double) 0);
        metric.getFlowUnits().stream()
                .filter(flowUnit -> !flowUnit.isEmpty() && !flowUnit.getData().isEmpty())
                .mapToDouble(
                        flowUnit ->
                                readDataFromSqlResult(
                                        flowUnit.getData(), field, fieldName, dataField))
                .forEach(
                        metricResponse -> {
                            if (Double.isNaN(metricResponse)) {
                                LOG.debug(
                                        "[AdmissionControl] Failed to parse metric from {}",
                                        metric.name());
                            } else {
                                metricValue.set(metricResponse);
                            }
                        });
        return metricValue.get();
    }

    private HeapMetrics getHeapMetric() {
        HeapMetrics heapMetrics = new HeapMetrics();
        heapMetrics.usedHeap =
                getMetric(heapUsedValue, MEM_TYPE.getField(), HEAP.toString(), MetricsDB.MAX)
                        / BYTES_TO_MEGABYTES;
        heapMetrics.maxHeap =
                getMetric(heapMaxValue, MEM_TYPE.getField(), HEAP.toString(), MetricsDB.MAX)
                        / BYTES_TO_MEGABYTES;
        return heapMetrics;
    }

    /**
     * read threshold values from rca.conf
     *
     * @param conf RcaConf object
     */
    @Override
    public void readRcaConf(RcaConf conf) {
        AdmissionControlRcaConfig rcaConfig = conf.getAdmissionControlRcaConfig();
        AdmissionControlRcaConfig.ControllerConfig requestSizeConfig =
                rcaConfig.getRequestSizeControllerConfig();
        List<Range> requestSizeHeapRangeConfiguration =
                requestSizeConfig.getHeapRangeConfiguration();
        if (requestSizeHeapRangeConfiguration != null
                && requestSizeHeapRangeConfiguration.size() > 0) {
            requestSizeHeapRange.setRangeConfiguration(requestSizeHeapRangeConfiguration);
        }
    }

    @Override
    public ResourceFlowUnit operate() {
        long currentTimeMillis = System.currentTimeMillis();

        counter++;
        if (counter < rcaPeriod) {
            return new ResourceFlowUnit<>(currentTimeMillis);
        }
        counter = 0;

        HeapMetrics heapMetrics = getHeapMetric();
        if (heapMetrics.usedHeap == 0 || heapMetrics.maxHeap == 0) {
            return new ResourceFlowUnit<>(currentTimeMillis);
        }
        double currentHeapPercent = (heapMetrics.usedHeap / heapMetrics.maxHeap) * 100;

        // If we observe heap percent range change then we tune request-size controller threshold
        // by marking resource as unhealthy and setting desired value as configured
        if (requestSizeHeapRange.hasRangeChanged(previousHeapPercent, currentHeapPercent)) {
            double desiredThreshold = getHeapBasedThreshold(currentHeapPercent);
            if (desiredThreshold == 0) {
                // AdmissionControl rejects all requests if threshold is set to 0, thus ignoring
                return new ResourceFlowUnit<>(currentTimeMillis);
            }
            LOG.debug(
                    "[AdmissionControl] Observed range change. previousHeapPercent={} currentHeapPercent={} desiredThreshold={}",
                    previousHeapPercent,
                    currentHeapPercent,
                    desiredThreshold);

            previousHeapPercent = currentHeapPercent;

            InstanceDetails instanceDetails = getInstanceDetails();
            HotNodeSummary nodeSummary =
                    new HotNodeSummary(
                            instanceDetails.getInstanceId(), instanceDetails.getInstanceIp());
            HotResourceSummary resourceSummary =
                    new HotResourceSummary(HEAP_MAX_SIZE, desiredThreshold, currentHeapPercent, 0);
            nodeSummary.appendNestedSummary(resourceSummary);

            RCA_VERTICES_METRICS_AGGREGATOR.updateStat(
                    ADMISSION_CONTROL_RCA_TRIGGERED, instanceDetails.getInstanceId().toString(), 1);

            return new ResourceFlowUnit<>(
                    currentTimeMillis,
                    new ResourceContext(UNHEALTHY),
                    nodeSummary,
                    !instanceDetails.getIsMaster());
        }

        return new ResourceFlowUnit<>(currentTimeMillis);
    }

    private double getHeapBasedThreshold(double currentHeapPercent) {
        Range range = requestSizeHeapRange.getRange(currentHeapPercent);
        return Objects.isNull(range) ? 0 : range.getThreshold();
    }

    public RangeConfiguration getRequestSizeHeapRange() {
        return this.requestSizeHeapRange;
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

    private static class HeapMetrics {
        private double usedHeap;
        private double maxHeap;
    }
}
