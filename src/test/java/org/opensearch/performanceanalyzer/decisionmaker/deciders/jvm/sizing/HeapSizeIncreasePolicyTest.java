/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.jvm.sizing;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.actions.HeapSizeIncreaseAction;
import org.opensearch.performanceanalyzer.rca.configs.HeapSizeIncreasePolicyConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.store.rca.jvmsizing.LargeHeapClusterRca;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    HeapSizeIncreasePolicy.class,
    HeapSizeIncreasePolicyTest.class,
    HeapSizeIncreaseAction.class
})
@PowerMockIgnore({
    "com.sun.org.apache.xerces.*",
    "javax.xml.*",
    "org.xml.*",
    "javax.management.*",
    "org.w3c.*"
})
public class HeapSizeIncreasePolicyTest {

    @Mock private LargeHeapClusterRca mockLargeHeapClusterRca;

    @Mock private AppContext mockAppContext;

    @Mock private RcaConf mockRcaConf;

    @Mock private HeapSizeIncreasePolicyConfig config;

    @Mock private Runtime mockRuntime;

    private HeapSizeIncreasePolicy testPolicy;
    private List<InstanceDetails> dataNodes;
    private InstanceDetails currentInstance;
    private static final int UNHEALTHY_NODE_PERCENTAGE = 50;
    private static final int DAY_BREACH = 8;
    private static final int WEEK_BREACH = 3;

    @Before
    public void setUp() throws Exception {
        this.currentInstance =
                new InstanceDetails(
                        new InstanceDetails.Id("current"), new InstanceDetails.Ip("2.3.4.5"), 0);
        initMocks(this);
        setupDataNodes();
        setupMockAppContext();
        setupMockRcaConf();
        PerformanceAnalyzerApp.initAggregators();
        testPolicy = new HeapSizeIncreasePolicy(mockLargeHeapClusterRca);
        testPolicy.setAppContext(mockAppContext);
        testPolicy.setRcaConf(mockRcaConf);
    }

    @Test
    public void testEvaluate() throws Exception {
        preWarmPolicy(DAY_BREACH + 1, WEEK_BREACH);
        List<Action> actions = testPolicy.evaluate();
        assertEquals(1, actions.size());
    }

    @Test
    public void testEvaluateInsufficientDayBreach() throws Exception {
        preWarmPolicy(DAY_BREACH - 2, WEEK_BREACH);
        List<Action> actions = testPolicy.evaluate();
        assertEquals(0, actions.size());
    }

    @Test
    public void testEvaluateInsufficientWeekBreach() throws Exception {
        preWarmPolicy(DAY_BREACH, WEEK_BREACH - 1);
        List<Action> actions = testPolicy.evaluate();
        assertEquals(0, actions.size());
    }

    @Test
    public void testReadFromConf() throws Exception {
        testPolicy.setRcaConf(mockRcaConf);

        assertEquals(UNHEALTHY_NODE_PERCENTAGE, testPolicy.getUnhealthyNodePercentage());
    }

    private void setupDataNodes() {
        InstanceDetails dataNode1 =
                new InstanceDetails(
                        new InstanceDetails.Id("node1"), new InstanceDetails.Ip("1.2.3.4"), 0);
        InstanceDetails dataNode2 =
                new InstanceDetails(
                        new InstanceDetails.Id("node2"), new InstanceDetails.Ip("1.2.3.5"), 0);
        InstanceDetails dataNode3 =
                new InstanceDetails(
                        new InstanceDetails.Id("node3"), new InstanceDetails.Ip("1.2.3.6"), 0);

        dataNodes = Arrays.asList(dataNode1, dataNode2, dataNode3);
    }

    private void preWarmPolicy(int dayBreach, int weekBreach) {
        mockStatic(System.class);
        Clock clk = Clock.systemUTC();
        Instant thisInstant = clk.instant();
        for (int i = 0; i < weekBreach; ++i) {
            Instant thisDay = thisInstant;
            for (int j = 0; j < dayBreach; ++j) {
                thisDay = thisDay.plus(60, ChronoUnit.MINUTES);
                evalAt(thisDay);
            }
            thisInstant = thisInstant.plus(1, ChronoUnit.DAYS);
        }
    }

    private void setupMockAppContext() {
        when(mockAppContext.getDataNodeInstances()).thenReturn(dataNodes);
        when(mockAppContext.getMyInstanceDetails()).thenReturn(currentInstance);
    }

    private void setupMockRcaConf() {
        when(mockRcaConf.getJvmScaleUpPolicyConfig()).thenReturn(config);
        when(config.getUnhealthyNodePercentage()).thenReturn(UNHEALTHY_NODE_PERCENTAGE);
        when(config.getDayBreachThreshold()).thenReturn(DAY_BREACH);
        when(config.getWeekBreachThreshold()).thenReturn(WEEK_BREACH);
    }

    private void evalAt(Instant currentInstant) {
        PowerMockito.when(System.currentTimeMillis()).thenReturn(currentInstant.toEpochMilli());
        HotClusterSummary hotClusterSummary =
                new HotClusterSummary(dataNodes.size(), dataNodes.size());
        for (InstanceDetails dataNode : dataNodes) {
            HotNodeSummary hotNodeSummary =
                    new HotNodeSummary(dataNode.getInstanceId(), dataNode.getInstanceIp());
            hotClusterSummary.appendNestedSummary(hotNodeSummary);
        }
        ResourceContext ctxt = new ResourceContext(Resources.State.CONTENDED);
        ResourceFlowUnit<HotClusterSummary> fu =
                new ResourceFlowUnit<>(currentInstant.toEpochMilli(), ctxt, hotClusterSummary);
        when(mockLargeHeapClusterRca.getFlowUnits()).thenReturn(Collections.singletonList(fu));
        testPolicy.evaluate();
    }
}
