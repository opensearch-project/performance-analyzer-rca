/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.admissioncontrol;

import static org.opensearch.performanceanalyzer.metrics.AllMetrics.GCType.HEAP;
import static org.opensearch.performanceanalyzer.metrics.AllMetrics.HeapDimension.MEM_TYPE;
import static org.opensearch.performanceanalyzer.rca.framework.api.Resources.State.HEALTHY;
import static org.opensearch.performanceanalyzer.rca.framework.api.persist.SQLParsingUtil.readDataFromSqlResult;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Field;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.configs.AdmissionControlRcaConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.scheduler.FlowUnitOperationArgWrapper;
import org.opensearch.performanceanalyzer.rca.store.rca.admissioncontrol.heap.AdmissionControlByHeap;
import org.opensearch.performanceanalyzer.rca.store.rca.admissioncontrol.heap.AdmissionControlByHeapFactory;
import org.opensearch.performanceanalyzer.rca.store.rca.admissioncontrol.model.HeapMetric;
import org.opensearch.performanceanalyzer.util.range.Range;
import org.opensearch.performanceanalyzer.util.range.RangeConfiguration;
import org.opensearch.performanceanalyzer.util.range.RequestSizeHeapRangeConfiguration;

public class AdmissionControlRca extends Rca<ResourceFlowUnit<HotNodeSummary>> {
    private static final Logger LOG = LogManager.getLogger(AdmissionControlRca.class);
    private static final double BYTES_TO_GIGABYTES = Math.pow(1024, 3);

    // Global JVM Memory Pressure Metric
    public static final String GLOBAL_JVMMP = "Global_JVMMP";
    // Request Size Metric
    public static final String REQUEST_SIZE = "Request_Size";

    private final Metric heapUsedValue;
    private final Metric heapMaxValue;

    private final RangeConfiguration requestSizeHeapRange;
    private final int rcaPeriod;
    private int counter;

    public <M extends Metric> AdmissionControlRca(
            final int rcaPeriodInSeconds, final M heapUsedValue, final M heapMaxValue) {
        super(rcaPeriodInSeconds);
        this.counter = 0;
        this.rcaPeriod = rcaPeriodInSeconds;
        this.heapUsedValue = heapUsedValue;
        this.heapMaxValue = heapMaxValue;
        this.requestSizeHeapRange = new RequestSizeHeapRangeConfiguration();
    }

    private <M extends Metric> double getMetric(M metric, Field<String> field, String fieldName) {
        double response = 0;
        for (MetricFlowUnit flowUnit : metric.getFlowUnits()) {
            if (!flowUnit.isEmpty()) {
                double metricResponse =
                        readDataFromSqlResult(flowUnit.getData(), field, fieldName, MetricsDB.MAX);
                if (!Double.isNaN(metricResponse) && metricResponse > 0) {
                    response = metricResponse;
                }
            }
        }
        return response;
    }

    private HeapMetric getHeapMetric() {
        double usedHeapInGb =
                getMetric(heapUsedValue, MEM_TYPE.getField(), HEAP.toString()) / BYTES_TO_GIGABYTES;
        double maxHeapInGb =
                getMetric(heapMaxValue, MEM_TYPE.getField(), HEAP.toString()) / BYTES_TO_GIGABYTES;
        return new HeapMetric(usedHeapInGb, maxHeapInGb);
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
    public ResourceFlowUnit<HotNodeSummary> operate() {
        long currentTimeMillis = System.currentTimeMillis();

        counter++;
        if (counter < rcaPeriod) {
            return new ResourceFlowUnit<>(currentTimeMillis);
        }
        counter = 0;

        InstanceDetails instanceDetails = getInstanceDetails();
        HotNodeSummary nodeSummary =
                new HotNodeSummary(
                        instanceDetails.getInstanceId(), instanceDetails.getInstanceIp());

        HeapMetric heapMetric = getHeapMetric();
        if (!heapMetric.hasValues()) {
            return new ResourceFlowUnit<>(
                    currentTimeMillis,
                    new ResourceContext(HEALTHY),
                    nodeSummary,
                    !instanceDetails.getIsClusterManager());
        }

        AdmissionControlByHeap admissionControlByHeap =
                AdmissionControlByHeapFactory.getByMaxHeap(heapMetric.getMaxHeap());
        admissionControlByHeap.init(instanceDetails, requestSizeHeapRange);
        return admissionControlByHeap.generateFlowUnits(heapMetric);
    }

    public RangeConfiguration getRequestSizeHeapRange() {
        return this.requestSizeHeapRange;
    }

    @Override
    public void generateFlowUnitListFromWire(FlowUnitOperationArgWrapper args) {
        final List<FlowUnitMessage> flowUnitMessages =
                args.getWireHopper().readFromWire(args.getNode());
        final List<ResourceFlowUnit<HotNodeSummary>> flowUnitList = new ArrayList<>();
        LOG.debug("rca: Executing fromWire: {}", this.getClass().getSimpleName());
        for (FlowUnitMessage flowUnitMessage : flowUnitMessages) {
            flowUnitList.add(ResourceFlowUnit.buildFlowUnitFromWrapper(flowUnitMessage));
        }
        setFlowUnits(flowUnitList);
    }
}
