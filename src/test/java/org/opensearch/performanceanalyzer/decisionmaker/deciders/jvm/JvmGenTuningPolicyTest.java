/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.jvm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.actions.JvmGenAction;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.DeciderConfig;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.jvm.young_gen.JvmGenTuningPolicyConfig;
import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.store.collector.NodeConfigCache;
import org.opensearch.performanceanalyzer.rca.store.rca.HighHeapUsageClusterRca;

public class JvmGenTuningPolicyTest {
    private JvmGenTuningPolicy policy;
    @Mock private HighHeapUsageClusterRca rca;
    @Mock private JvmGenTuningPolicyConfig policyConfig;
    @Mock private JvmActionsAlarmMonitor tooSmallAlarm;
    @Mock private JvmActionsAlarmMonitor tooLargeAlarm;
    @Mock private RcaConf rcaConf;
    @Mock private AppContext appContext;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        MockitoAnnotations.initMocks(this);
        policy = new JvmGenTuningPolicy(rca, tooSmallAlarm, tooLargeAlarm);
        policy.setRcaConf(rcaConf);
        policy.setAppContext(appContext);
        DeciderConfig deciderConfig = mock(DeciderConfig.class);
        when(rcaConf.getDeciderConfig()).thenReturn(deciderConfig);
        when(deciderConfig.getJvmGenTuningPolicyConfig()).thenReturn(policyConfig);
        ResourceFlowUnit<HotClusterSummary> flowUnit =
                (ResourceFlowUnit<HotClusterSummary>) mock(ResourceFlowUnit.class);
        when(rca.getFlowUnits()).thenReturn(Collections.singletonList(flowUnit));
    }

    /**
     * After a call to this function, rca will return a FlowUnit containing n issues with resource
     *
     * <p>This is useful because the policy suggests actions based on resources and the count of
     * issues observed for those resources.
     *
     * @param n the number of young gen issues
     */
    private void mockRcaIssues(int n, Resource resource) {
        ResourceFlowUnit<HotClusterSummary> flowUnit = new ResourceFlowUnit<>(1);
        HotNodeSummary nodeSummary =
                new HotNodeSummary(
                        new InstanceDetails.Id("A"), new InstanceDetails.Ip("127.0.0.1"));
        for (int i = 0; i < n; i++) {
            nodeSummary.appendNestedSummary(new HotResourceSummary(resource, 1, 1, 1));
        }
        HotClusterSummary hotClusterSummary = new HotClusterSummary(3, 1);
        hotClusterSummary.appendNestedSummary(nodeSummary);
        flowUnit.setSummary(hotClusterSummary);
        when(rca.getFlowUnits()).thenReturn(Collections.singletonList(flowUnit));
    }

    /**
     * Causes the policy to view the old:young gen size ratio as desiredRatio
     *
     * @param desiredRatio the old:young gen size ratio; 3 means the old gen is 3X larger than young
     */
    private void mockCurrentRatio(double desiredRatio) {
        NodeConfigCache cache = mock(NodeConfigCache.class);
        when(appContext.getNodeConfigCache()).thenReturn(cache);
        when(appContext.getDataNodeInstances())
                .thenReturn(
                        Collections.singletonList(
                                new InstanceDetails(
                                        AllMetrics.NodeRole.DATA,
                                        new InstanceDetails.Id("A"),
                                        new InstanceDetails.Ip("127.0.0.1"),
                                        false)));
        when(cache.get(any(), eq(ResourceUtil.OLD_GEN_MAX_SIZE))).thenReturn(desiredRatio);
        when(cache.get(any(), eq(ResourceUtil.YOUNG_GEN_MAX_SIZE))).thenReturn(1d);
    }

    /** When one of rcaConf, appContext is null, policy evaluation should return no actions */
    @Test
    public void testEvaluate_withoutContext() {
        policy.setRcaConf(null);
        Assert.assertTrue(policy.evaluate().isEmpty());
        policy.setAppContext(null);
        Assert.assertTrue(policy.evaluate().isEmpty());
        policy.setRcaConf(rcaConf);
        Assert.assertTrue(policy.evaluate().isEmpty());
    }

    /** Tests that the evaluate() method returns the correct actions in various scenarios */
    @Test
    public void testEvaluate() {
        // The policy should not emit actions when it is disabled
        when(policyConfig.isEnabled()).thenReturn(false);
        Assert.assertTrue(policy.evaluate().isEmpty());
        when(policyConfig.isEnabled()).thenReturn(true);
        // Neither generation has had enough issues
        mockRcaIssues(1, ResourceUtil.MINOR_GC_PAUSE_TIME);
        // The policy should not suggest any actions when there are no issues
        when(policyConfig.allowYoungGenDownsize()).thenReturn(false);
        Assert.assertTrue(policy.evaluate().isEmpty());
        verify(tooLargeAlarm, times(1)).recordIssue();
        reset(tooLargeAlarm);
        // Make the young generation seem oversized
        mockRcaIssues(10, ResourceUtil.MINOR_GC_PAUSE_TIME);
        // The policy should not suggest decreasing young gen when the option is disabled
        when(tooLargeAlarm.isHealthy()).thenReturn(false);
        when(policyConfig.allowYoungGenDownsize()).thenReturn(false);
        Assert.assertTrue(policy.evaluate().isEmpty());
        verify(tooLargeAlarm, times(10)).recordIssue();
        // The policy should suggest decreasing young gen when it is oversized and the option is
        // enabled
        when(policyConfig.allowYoungGenDownsize()).thenReturn(true);
        mockCurrentRatio(4);
        List<Action> actions = policy.evaluate();
        Assert.assertEquals(1, actions.size());
        Assert.assertTrue(actions.get(0) instanceof JvmGenAction);
        Assert.assertEquals(5, ((JvmGenAction) actions.get(0)).getTargetRatio());
        mockCurrentRatio(5);
        actions = policy.evaluate();
        Assert.assertEquals(1, actions.size());
        Assert.assertTrue(actions.get(0) instanceof JvmGenAction);
        Assert.assertEquals(6, ((JvmGenAction) actions.get(0)).getTargetRatio());
        // Should not decrease the young gen size if the ratio is beyond 5:1
        mockCurrentRatio(5.1);
        actions = policy.evaluate();
        Assert.assertTrue(actions.isEmpty());
        // Make the young generation seem undersized
        mockRcaIssues(10, ResourceUtil.YOUNG_GEN_PROMOTION_RATE);
        // The policy should suggest increasing young gen when it is undersized
        mockCurrentRatio(50);
        when(tooLargeAlarm.isHealthy()).thenReturn(true);
        when(tooSmallAlarm.isHealthy()).thenReturn(false);
        actions = policy.evaluate();
        verify(tooSmallAlarm, times(10)).recordIssue();
        Assert.assertEquals(1, actions.size());
        Assert.assertTrue(actions.get(0) instanceof JvmGenAction);
        Assert.assertEquals(3, ((JvmGenAction) actions.get(0)).getTargetRatio());
        mockCurrentRatio(5);
        actions = policy.evaluate();
        Assert.assertEquals(1, actions.size());
        Assert.assertTrue(actions.get(0) instanceof JvmGenAction);
        Assert.assertEquals(4, ((JvmGenAction) actions.get(0)).getTargetRatio());
        mockCurrentRatio(4);
        actions = policy.evaluate();
        Assert.assertEquals(1, actions.size());
        Assert.assertTrue(actions.get(0) instanceof JvmGenAction);
        Assert.assertEquals(3, ((JvmGenAction) actions.get(0)).getTargetRatio());
        // Should not increase the young gen size if the ratio is not beyond 4:1
        mockCurrentRatio(3.9);
        actions = policy.evaluate();
        Assert.assertTrue(actions.isEmpty());
    }
}
