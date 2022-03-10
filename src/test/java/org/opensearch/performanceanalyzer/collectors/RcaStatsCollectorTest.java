/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.collectors;


import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.metrics.MetricsConfiguration;
import org.opensearch.performanceanalyzer.rca.RcaTestHelper;
import org.opensearch.performanceanalyzer.rca.framework.api.AnalysisGraph;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.Symptom;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.SymptomFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.CPU_Utilization;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Heap_AllocRate;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Paging_MajfltRate;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Sched_Waittime;
import org.opensearch.performanceanalyzer.rca.framework.core.ConnectedComponent;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.metrics.JvmMetrics;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaGraphMetrics;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaUtil;
import org.opensearch.performanceanalyzer.rca.scheduler.RCASchedulerTask;
import org.opensearch.performanceanalyzer.rca.spec.MetricsDBProviderTestHelper;
import org.opensearch.performanceanalyzer.rca.stats.emitters.PeriodicSamplers;
import org.opensearch.performanceanalyzer.rca.stats.measurements.MeasurementSet;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;

public class RcaStatsCollectorTest {
    class FaultyAnalysisGraph extends AnalysisGraph {
        @Override
        public void construct() {
            Metric cpuUtilization = new CPU_Utilization(1);
            Metric heapUsed = new Sched_Waittime(1);
            Metric pageMaj = new Paging_MajfltRate(1);
            Metric heapAlloc = new Heap_AllocRate(1);

            addLeaf(cpuUtilization);
            addLeaf(heapUsed);
            addLeaf(pageMaj);
            addLeaf(heapAlloc);

            Symptom s1 = new HighCpuSymptom(1, cpuUtilization, heapUsed);
            s1.addAllUpstreams(Arrays.asList(cpuUtilization, heapUsed));

            System.out.println(this.getClass().getName() + " graph constructed..");
        }

        class HighCpuSymptom extends Symptom {
            Metric cpu;
            Metric heapUsed;

            public HighCpuSymptom(long evaluationIntervalSeconds, Metric cpu, Metric heapUsed) {
                super(evaluationIntervalSeconds);
                this.cpu = cpu;
                this.heapUsed = heapUsed;
            }

            @Override
            public SymptomFlowUnit operate() {
                int x = 5 / 0;
                return new SymptomFlowUnit(0L);
            }
        }
    }

    @Test
    public void rcaGraphMetrics() throws Exception {
        RcaTestHelper.cleanUpLogs();

        RcaGraphMetrics graphMetrics[] = {
            RcaGraphMetrics.GRAPH_NODE_OPERATE_CALL,
            RcaGraphMetrics.METRIC_GATHER_CALL,
            RcaGraphMetrics.NUM_GRAPH_NODES,
            RcaGraphMetrics.NUM_NODES_EXECUTED_LOCALLY,
        };

        JvmMetrics jvmMetrics[] = {
            JvmMetrics.JVM_FREE_MEM_SAMPLER, JvmMetrics.JVM_TOTAL_MEM_SAMPLER
        };

        List<ConnectedComponent> connectedComponents =
                RcaUtil.getAnalysisGraphComponents(new FaultyAnalysisGraph());
        RcaConf rcaConf = new RcaConf(Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca.conf").toString());

        ClusterDetailsEventProcessor clusterDetailsEventProcessor =
                new ClusterDetailsEventProcessor();
        clusterDetailsEventProcessor.setNodesDetails(
                Collections.singletonList(
                        new ClusterDetailsEventProcessor.NodeDetails(
                                AllMetrics.NodeRole.UNKNOWN, "node1", "127.0.0.1", false)));
        AppContext appContext = new AppContext();
        appContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);

        RCASchedulerTask rcaSchedulerTask =
                new RCASchedulerTask(
                        1000,
                        Executors.newFixedThreadPool(2),
                        connectedComponents,
                        new MetricsDBProviderTestHelper(true),
                        null,
                        rcaConf,
                        null,
                        appContext);
        rcaSchedulerTask.run();
        StatsCollector statsCollector = new StatsCollector("test-stats", 0, new HashMap<>());

        for (RcaGraphMetrics metricToCheck : graphMetrics) {
            Assert.assertTrue(verify(metricToCheck));
        }
        for (JvmMetrics jvmMetrics1 : jvmMetrics) {
            if (!verify(jvmMetrics1)) {
                PerformanceAnalyzerApp.PERIODIC_SAMPLERS =
                        new PeriodicSamplers(
                                PerformanceAnalyzerApp.PERIODIC_SAMPLE_AGGREGATOR,
                                PerformanceAnalyzerApp.getAllSamplers(appContext),
                                (MetricsConfiguration.CONFIG_MAP.get(StatsCollector.class)
                                                .samplingInterval)
                                        / 2,
                                TimeUnit.MILLISECONDS);

                PerformanceAnalyzerApp.PERIODIC_SAMPLERS.run();
            }
            Assert.assertTrue(verify(jvmMetrics1));
        }
        statsCollector.collectMetrics(0);
    }

    private boolean verify(MeasurementSet measurementSet) throws InterruptedException {
        final int MAX_TIME_TO_WAIT_MILLIS = 10_000;
        int waited_for_millis = 0;
        while (waited_for_millis++ < MAX_TIME_TO_WAIT_MILLIS) {
            if (PerformanceAnalyzerApp.RCA_STATS_REPORTER.isMeasurementCollected(measurementSet)) {
                return true;
            }
            Thread.sleep(1);
        }
        System.out.println("Could not find measurement " + measurementSet);
        return false;
    }

    @After
    public void cleanup() {
        RcaTestHelper.cleanUpLogs();
    }
}
