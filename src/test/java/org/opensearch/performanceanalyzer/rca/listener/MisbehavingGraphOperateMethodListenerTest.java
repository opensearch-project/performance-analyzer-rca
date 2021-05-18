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
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.collectors.StatsCollector;
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
import org.opensearch.performanceanalyzer.rca.stats.measurements.MeasurementSet;

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
            Assert.assertTrue(verify(ExceptionsAndErrors.EXCEPTION_IN_OPERATE));
        }

        Assert.assertEquals(1, Stats.getInstance().getMutedGraphNodesCount());
        Assert.assertTrue(
                Stats.getInstance()
                        .isNodeMuted(FaultyAnalysisGraph.HighCpuSymptom.class.getSimpleName()));
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
        return false;
    }

    @After
    public void cleanup() {
        // RcaTestHelper.cleanUpLogs();
    }
}
