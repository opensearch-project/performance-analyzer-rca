/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.listener;


import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.commons.collectors.StatsCollector;
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
import org.opensearch.performanceanalyzer.rca.framework.core.Stats;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ExceptionsAndErrors;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaUtil;
import org.opensearch.performanceanalyzer.rca.scheduler.RCASchedulerTask;
import org.opensearch.performanceanalyzer.rca.spec.MetricsDBProviderTestHelper;

public class MisbehavingGraphOperateMethodListenerTest {
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

            Symptom s1 =
                    new MisbehavingGraphOperateMethodListenerTest.FaultyAnalysisGraph
                            .HighCpuSymptom(1, cpuUtilization, heapUsed);
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
    public void rcaMutedForThrowingExceptions() throws Exception {
        StatsCollector statsCollector = new StatsCollector("test-stats", 0, new HashMap<>());
        statsCollector.collectMetrics(0);
        RcaTestHelper.cleanUpLogs();

        List<ConnectedComponent> connectedComponents =
                RcaUtil.getAnalysisGraphComponents(
                        new MisbehavingGraphOperateMethodListenerTest.FaultyAnalysisGraph());
        RcaConf rcaConf = new RcaConf(Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca.conf").toString());

        RCASchedulerTask rcaSchedulerTask =
                new RCASchedulerTask(
                        1000,
                        Executors.newFixedThreadPool(2),
                        connectedComponents,
                        new MetricsDBProviderTestHelper(true),
                        null,
                        rcaConf,
                        null,
                        new AppContext());

        for (int i = 0; i <= MisbehavingGraphOperateMethodListener.TOLERANCE_LIMIT; i++) {
            rcaSchedulerTask.run();
            Assert.assertTrue(RcaTestHelper.verify(ExceptionsAndErrors.EXCEPTION_IN_OPERATE));
        }

        Assert.assertEquals(1, Stats.getInstance().getMutedGraphNodesCount());
        Assert.assertTrue(
                Stats.getInstance()
                        .isNodeMuted(FaultyAnalysisGraph.HighCpuSymptom.class.getSimpleName()));
    }

    @After
    public void cleanup() {
        // RcaTestHelper.cleanUpLogs();
    }
}
