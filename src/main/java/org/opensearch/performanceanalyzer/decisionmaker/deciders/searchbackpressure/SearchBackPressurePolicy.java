/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.searchbackpressure;

import static org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil.SEARCHBACKPRESSURE_SHARD;
import static org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil.SEARCHBACKPRESSURE_TASK;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.DecisionPolicy;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.searchbackpressure.SearchBackPressurePolicyConfig;
import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.store.rca.searchbackpressure.SearchBackPressureClusterRCA;

/**
 * Decides if the SearchBackPressure threshold should be modified suggests actions to take to
 * achieve improved performance.
 */
public class SearchBackPressurePolicy implements DecisionPolicy {
    private static final Logger LOG = LogManager.getLogger(SearchBackPressurePolicy.class);

    /* Specify a path to store SearchBackpressurePolicy_Autotune Stats */

    /* TO DO: Check which settings should be modifed based on search heap shard/task cancellation stats */
    boolean searchTaskHeapThresholdShouldChange;

    private AppContext appContext;
    private RcaConf rcaConf;
    private SearchBackPressurePolicyConfig policyConfig;
    private SearchBackPressureClusterRCA searchBackPressureClusterRCA;

    /* Hourly Alarm frequency threshold */
    private int hourlyAlarmThreshold;

    /* Alarm for heap usage */
    static final List<Resource> HEAP_SEARCHBP_SIGNALS =
            Lists.newArrayList(SEARCHBACKPRESSURE_SHARD, SEARCHBACKPRESSURE_TASK);

    /* Hourly heap used threshold */
    @VisibleForTesting SearchBpActionsAlarmMonitor searchBackPressureHeapAlarm;

    public SearchBackPressurePolicy(
            SearchBackPressureClusterRCA searchBackPressureClusterRCA,
            SearchBpActionsAlarmMonitor searchBackPressureHeapAlarm) {
        this.searchBackPressureClusterRCA = searchBackPressureClusterRCA;
        this.searchBackPressureHeapAlarm = searchBackPressureHeapAlarm;
    }

    public SearchBackPressurePolicy(SearchBackPressureClusterRCA searchBackPressureClusterRCA) {
        this(searchBackPressureClusterRCA, null);
    }

    /**
     * records issues which the policy cares about and discards others
     *
     * @param issue an issue with the application
     */
    private void record(HotResourceSummary issue) {
        LOG.debug("SearchBackPressurePolicy#record()");
        if (HEAP_SEARCHBP_SIGNALS.contains(issue.getResource())) {
            LOG.debug("Recording issue in searchBackPressureHeapAlarm");
            searchBackPressureHeapAlarm.recordIssue();
        }
    }

    /** gathers and records all issues observed in the application */
    private void recordIssues() {
        if (searchBackPressureClusterRCA.getFlowUnits().isEmpty()) {
            return;
        }
        for (ResourceFlowUnit<HotClusterSummary> flowUnit :
                searchBackPressureClusterRCA.getFlowUnits()) {
            if (!flowUnit.hasResourceSummary()) {
                continue;
            }
            HotClusterSummary clusterSummary = flowUnit.getSummary();
            for (HotNodeSummary nodeSummary : clusterSummary.getHotNodeSummaryList()) {
                for (HotResourceSummary summary : nodeSummary.getHotResourceSummaryList()) {
                    record(summary);
                }
            }
        }
    }

    /* TO DO: Change the logic of heapThresholdIsTooSmall */
    public boolean heapThresholdIsTooSmall(){
        return !searchBackPressureHeapAlarm.isHealthy();
    }

    /* TO DO: Change the logic of heapThresholdIsTooLarge */
    public boolean heapThresholdIsTooLarge(){
        return !searchBackPressureHeapAlarm.isHealthy();
    }

    // create alarm monitor from config

    // initalize all alarm monitors



    @Override
    public List<Action> evaluate() {
        return null;
    }

    public void setAppContext(AppContext appContext) {
        this.appContext = appContext;
    }

    public void setRcaConf(final RcaConf rcaConf) {
        this.rcaConf = rcaConf;
    }
}
