/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.jvm.sizing;


import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.actions.HeapSizeIncreaseAction;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.AlarmMonitor;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.DecisionPolicy;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.jvm.JvmActionsAlarmMonitor;
import org.opensearch.performanceanalyzer.rca.configs.HeapSizeIncreasePolicyConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaRuntimeMetrics;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.NodeKey;
import org.opensearch.performanceanalyzer.rca.store.rca.jvmsizing.LargeHeapClusterRca;

public class HeapSizeIncreasePolicy implements DecisionPolicy {

    private final LargeHeapClusterRca largeHeapClusterRca;
    private AppContext appContext;
    private RcaConf rcaConf;
    private final HeapSizeIncreaseClusterMonitor heapSizeIncreaseClusterMonitor;

    private int unhealthyNodePercentage;

    public HeapSizeIncreasePolicy(final LargeHeapClusterRca largeHeapClusterRca) {
        this.heapSizeIncreaseClusterMonitor = new HeapSizeIncreaseClusterMonitor();
        this.largeHeapClusterRca = largeHeapClusterRca;
    }

    @Override
    public List<Action> evaluate() {
        addToClusterMonitor();

        List<Action> actions = new ArrayList<>();
        if (!heapSizeIncreaseClusterMonitor.isHealthy()) {
            Action heapSizeIncreaseAction = new HeapSizeIncreaseAction(appContext);
            if (heapSizeIncreaseAction.isActionable()) {
                PerformanceAnalyzerApp.RCA_RUNTIME_METRICS_AGGREGATOR.updateStat(
                        RcaRuntimeMetrics.HEAP_SIZE_INCREASE_ACTION_SUGGESTED, "", 1);
                actions.add(heapSizeIncreaseAction);
            }
        }

        return actions;
    }

    private void addToClusterMonitor() {
        long currTime = System.currentTimeMillis();
        if (largeHeapClusterRca.getFlowUnits().isEmpty()) {
            return;
        }
        ResourceFlowUnit<HotClusterSummary> flowUnit = largeHeapClusterRca.getFlowUnits().get(0);

        if (flowUnit.getSummary() == null) {
            return;
        }
        List<HotNodeSummary> hotNodeSummaries = flowUnit.getSummary().getHotNodeSummaryList();
        hotNodeSummaries.forEach(
                hotNodeSummary -> {
                    NodeKey nodeKey =
                            new NodeKey(
                                    hotNodeSummary.getNodeID(), hotNodeSummary.getHostAddress());
                    heapSizeIncreaseClusterMonitor.recordIssue(nodeKey, currTime);
                });
    }

    private class HeapSizeIncreaseClusterMonitor {

        private static final int DEFAULT_DAY_BREACH_THRESHOLD = 8;
        private static final int DEFAULT_WEEK_BREACH_THRESHOLD = 3;
        private static final String PERSISTENCE_PREFIX = "heap-size-increase-alarm-";
        private final Map<NodeKey, AlarmMonitor> perNodeMonitor;
        private int dayBreachThreshold = DEFAULT_DAY_BREACH_THRESHOLD;
        private int weekBreachThreshold = DEFAULT_WEEK_BREACH_THRESHOLD;

        HeapSizeIncreaseClusterMonitor() {
            this.perNodeMonitor = new HashMap<>();
        }

        public void recordIssue(final NodeKey nodeKey, long currTimeStamp) {
            perNodeMonitor
                    .computeIfAbsent(
                            nodeKey,
                            key ->
                                    new JvmActionsAlarmMonitor(
                                            dayBreachThreshold,
                                            weekBreachThreshold,
                                            Paths.get(
                                                    RcaConsts.CONFIG_DIR_PATH,
                                                    PERSISTENCE_PREFIX
                                                            + key.getNodeId().toString())))
                    .recordIssue(currTimeStamp, 1d);
        }

        public boolean isHealthy() {
            int numDataNodesInCluster = appContext.getDataNodeInstances().size();
            double unhealthyCount = 0;
            for (final AlarmMonitor monitor : perNodeMonitor.values()) {
                if (!monitor.isHealthy()) {
                    unhealthyCount++;
                }
            }
            return (unhealthyCount / numDataNodesInCluster) * 100d < unhealthyNodePercentage;
        }

        public void setDayBreachThreshold(int dayBreachThreshold) {
            this.dayBreachThreshold = dayBreachThreshold;
        }

        public void setWeekBreachThreshold(int weekBreachThreshold) {
            this.weekBreachThreshold = weekBreachThreshold;
        }
    }

    public void setAppContext(@Nonnull final AppContext appContext) {
        this.appContext = appContext;
    }

    public void setRcaConf(final RcaConf rcaConf) {
        this.rcaConf = rcaConf;
        readThresholdValuesFromConf();
    }

    private void readThresholdValuesFromConf() {
        HeapSizeIncreasePolicyConfig policyConfig = rcaConf.getJvmScaleUpPolicyConfig();
        this.unhealthyNodePercentage = policyConfig.getUnhealthyNodePercentage();
        this.heapSizeIncreaseClusterMonitor.setDayBreachThreshold(
                policyConfig.getDayBreachThreshold());
        this.heapSizeIncreaseClusterMonitor.setWeekBreachThreshold(
                policyConfig.getWeekBreachThreshold());
    }

    @VisibleForTesting
    public int getUnhealthyNodePercentage() {
        return unhealthyNodePercentage;
    }
}
