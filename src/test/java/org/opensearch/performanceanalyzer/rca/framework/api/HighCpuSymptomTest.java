/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jooq.Record;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.GradleTaskForRca;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.MovingAverage;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.SymptomContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.SymptomFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.CPU_Utilization;
import org.opensearch.performanceanalyzer.rca.framework.core.ConnectedComponent;
import org.opensearch.performanceanalyzer.rca.framework.core.Node;
import org.opensearch.performanceanalyzer.rca.framework.core.Queryable;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaUtil;
import org.opensearch.performanceanalyzer.rca.spec.MetricsDBProviderTestHelper;

@Category(GradleTaskForRca.class)
public class HighCpuSymptomTest {
    class CPU_UtilizationX extends CPU_Utilization {
        public CPU_UtilizationX(long evaluationIntervalSeconds) {
            super(evaluationIntervalSeconds);
        }

        public void setFlowUnitList(List<MetricFlowUnit> flowUnitList) {
            setFlowUnits(flowUnitList);
        }
    }

    class ShardCpuHighSymptom extends Symptom {
        private final Metric cpu_UtilizationX;

        public <M extends Metric> ShardCpuHighSymptom(
                long evaluationIntervalSeconds, M cpu_UtilizationX) {
            super(evaluationIntervalSeconds);
            this.cpu_UtilizationX = cpu_UtilizationX;
        }

        // The operate here tries to find hot shard. The way it tries to do it is creates a
        // MovingAverage object for
        // each distinct shardID. The moving average window is defined as three samples for less
        // that
        // three it
        // outputs -1 and for greater than or equal to 3, it outputs the average for that window.
        // This
        // method
        // compares it against an arbitrary threshold of 90% and if such a shard exists, then it
        // reports
        // it as a flow
        // unit. So the symptom can be defined as if there exists a shard for which the average of
        // max
        // is greater
        // than 90% for three consecutive samples, then classify it as hot. This is not accurate in
        // production but it
        // tests the system works for arbitrary samples of data.
        @Override
        public SymptomFlowUnit operate() {
            List<MetricFlowUnit> cpuMetrics = cpu_UtilizationX.getFlowUnits();
            boolean shouldReportOperation = false;

            MetricFlowUnit cpuMetric = cpuMetrics.get(0);
            Map<String, MovingAverage> averageMap = new HashMap<>();
            final double HIGH_CPU_THRESHOLD = 90.0;
            List<List<String>> ret = new ArrayList<>();
            if (cpuMetric.getData() != null) {
                for (Record record : cpuMetric.getData()) {
                    try {
                        String shardId =
                                record.getValue(
                                        AllMetrics.CommonDimension.SHARD_ID.toString(),
                                        String.class);
                        double data = record.getValue(MetricsDB.MAX, Double.class);
                        MovingAverage entry = averageMap.get(shardId);
                        if (null == entry) {
                            entry = new MovingAverage(3);
                            averageMap.put(shardId, entry);
                        }
                        double val = entry.next(data);
                        if (val > HIGH_CPU_THRESHOLD) {
                            List<String> dataRow = Collections.singletonList(shardId);
                            // context.put("threshold", String.valueOf(HIGH_CPU_THRESHOLD));
                            // context.put("actual", String.valueOf(val));
                            ret.add(dataRow);
                            System.out.println(
                                    String.format(
                                            "Shard %s is hot. Average max CPU (%f) above: %f",
                                            shardId, val, HIGH_CPU_THRESHOLD));
                            shouldReportOperation = true;
                        }
                    } catch (Exception e) {
                        System.out.println(String.format("Fail to parse data"));
                    }
                }
            }
            if (shouldReportOperation) {
                return new SymptomFlowUnit(
                        System.currentTimeMillis(),
                        ret,
                        new SymptomContext(Resources.State.UNHEALTHY));
            } else {
                return new SymptomFlowUnit(
                        System.currentTimeMillis(), new SymptomContext(Resources.State.HEALTHY));
            }
        }
    }

    class AnalysisGraphTest2 extends AnalysisGraph {
        @Override
        public void construct() {
            Metric metric = new CPU_UtilizationX(60);
            addLeaf(metric);
            Symptom symptom = new ShardCpuHighSymptom(60, metric);
            symptom.addAllUpstreams(Collections.singletonList(metric));
        }
    }

    @Test
    public void testSymptomCreation() throws Exception {
        Queryable queryable = new MetricsDBProviderTestHelper(false);

        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(
                        CPU_Utilization.NAME,
                        Arrays.asList("shard1", "index3", "bulk", "primary"),
                        92.4);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(
                        CPU_Utilization.NAME,
                        Arrays.asList("shard2", "index3", "bulk", "primary"),
                        93.4);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(
                        CPU_Utilization.NAME,
                        Arrays.asList("shard1", "index3", "bulk", "primary"),
                        95.4);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(
                        CPU_Utilization.NAME,
                        Arrays.asList("shard2", "index3", "bulk", "primary"),
                        4.4);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(
                        CPU_Utilization.NAME,
                        Arrays.asList("shard1", "index3", "bulk", "primary"),
                        90.4);
        ((MetricsDBProviderTestHelper) queryable)
                .addNewData(
                        CPU_Utilization.NAME,
                        Arrays.asList("shard2", "index3", "bulk", "primary"),
                        5.4);

        AnalysisGraph graph = new AnalysisGraphTest2();
        List<ConnectedComponent> components = RcaUtil.getAnalysisGraphComponents(graph);

        for (ConnectedComponent component : components) {
            for (List<Node<?>> nodeList : component.getAllNodesByDependencyOrder()) {
                for (Node<?> node : nodeList) {
                    System.out.println(node.name() + " -> " + node.getClass().getName());
                }
            }
        }

        for (ConnectedComponent component : components) {
            for (List<Node<?>> nodeList : component.getAllNodesByDependencyOrder()) {
                for (Node<?> node : nodeList) {
                    if (node instanceof CPU_UtilizationX) {
                        List<MetricFlowUnit> flowUnits =
                                Collections.singletonList(
                                        ((CPU_UtilizationX) node).gather(queryable));
                        ((CPU_UtilizationX) node).setFlowUnitList(flowUnits);
                    } else if (node instanceof Symptom) {
                        SymptomFlowUnit flowUnit = ((Symptom) node).operate();
                        assertEquals(
                                flowUnit.getData().get(0), Collections.singletonList("shard1"));
                        Assert.assertEquals(
                                flowUnit.getContext().getState(), Resources.State.UNHEALTHY);
                        /*
                        AssertHelper.compareMaps(new HashMap<String, String>() {{
                            this.put("threshold", "90.0");
                            this.put("actual", "92.7");
                        }}, flowUnit.getContextMap());
                         */
                    }
                }
            }
        }
    }
}
