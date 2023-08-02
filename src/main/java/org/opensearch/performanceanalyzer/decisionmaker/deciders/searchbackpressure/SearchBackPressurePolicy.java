/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.searchbackpressure;

import static org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil.SEARCHBACKPRESSURE_SHARD;
import static org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil.SEARCHBACKPRESSURE_TASK;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.actions.SearchBackPressureAction;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.DecisionPolicy;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.searchbackpressure.SearchBackPressurePolicyConfig;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.searchbackpressure.model.SearchBackPressureIssue;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.searchbackpressure.model.SearchBackPressureSearchTaskIssue;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.searchbackpressure.model.SearchBackPressureSearchTaskIssue.SearchbpTaskAlarmMonitorMapKeys;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.searchbackpressure.model.SearchBackPressureShardIssue;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.searchbackpressure.model.SearchBackPressureShardIssue.SearchbpShardAlarmMonitorMapKeys;
import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.BucketizedSlidingWindowConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;
import org.opensearch.performanceanalyzer.rca.store.rca.searchbackpressure.SearchBackPressureClusterRCA;

/**
 * Decides if the SearchBackPressure threshold should be modified suggests actions to take to
 * achieve improved performance.
 */
public class SearchBackPressurePolicy implements DecisionPolicy {
    private static final Logger LOG = LogManager.getLogger(SearchBackPressurePolicy.class);

    // Default COOLOFF Period for the action (1 DAY)
    private static final long DEAFULT_COOLOFF_PERIOD_IN_MILLIS = 24L * 60L * 60L * 1000L;
    private static final String HEAP_THRESHOLD_STR = "heap_usage";
    private static final String SHARD_DIMENSION_STR = "SHARD";
    private static final String TASK_DIMENSION_STR = "TASK";
    private static final double DEFAULT_HEAP_CHANGE_IN_PERCENTAGE = 5.0;

    private static final Path SEARCHBP_DATA_FILE_PATH =
            Paths.get(RcaConsts.CONFIG_DIR_PATH, "SearchBackPressurePolicy_heap");

    /* TODO: Specify a path to store SearchBackpressurePolicy_Autotune Stats */

    private AppContext appContext;
    private RcaConf rcaConf;
    private SearchBackPressurePolicyConfig policyConfig;
    private SearchBackPressureClusterRCA searchBackPressureClusterRCA;

    /* Alarm for heap usage */
    static final List<Resource> HEAP_SEARCHBP_SHARD_SIGNALS =
            Lists.newArrayList(SEARCHBACKPRESSURE_SHARD);
    static final List<Resource> HEAP_SEARCHBP_TASK_SIGNALS =
            Lists.newArrayList(SEARCHBACKPRESSURE_TASK);

    SearchBackPressureIssue searchBackPressureIssue;

    /* alarm monitors per threshold */
    // shard-level alarms
    @VisibleForTesting SearchBpActionsAlarmMonitor searchBackPressureShardHeapIncreaseAlarm;
    @VisibleForTesting SearchBpActionsAlarmMonitor searchBackPressureShardHeapDecreaseAlarm;
    HashMap<String, SearchBpActionsAlarmMonitor> searchBackPressureShardAlarmMonitorMap;

    // task-level alarms
    @VisibleForTesting SearchBpActionsAlarmMonitor searchBackPressureTaskHeapIncreaseAlarm;
    @VisibleForTesting SearchBpActionsAlarmMonitor searchBackPressureTaskHeapDecreaseAlarm;
    HashMap<String, SearchBpActionsAlarmMonitor> searchBackPressureTaskAlarmMonitorMap;

    public SearchBackPressurePolicy(
            SearchBackPressureClusterRCA searchBackPressureClusterRCA,
            SearchBpActionsAlarmMonitor searchBackPressureShardHeapIncreaseAlarm,
            SearchBpActionsAlarmMonitor searchBackPressureShardHeapDecreaseAlarm,
            SearchBpActionsAlarmMonitor searchBackPressureTaskHeapIncreaseAlarm,
            SearchBpActionsAlarmMonitor searchBackPressureTaskHeapDecreaseAlarm) {
        this.searchBackPressureClusterRCA = searchBackPressureClusterRCA;
        this.searchBackPressureShardHeapIncreaseAlarm = searchBackPressureShardHeapIncreaseAlarm;
        this.searchBackPressureShardHeapDecreaseAlarm = searchBackPressureShardHeapDecreaseAlarm;
        this.searchBackPressureTaskHeapIncreaseAlarm = searchBackPressureTaskHeapIncreaseAlarm;
        this.searchBackPressureTaskHeapDecreaseAlarm = searchBackPressureTaskHeapDecreaseAlarm;
    }

    public SearchBackPressurePolicy(SearchBackPressureClusterRCA searchBackPressureClusterRCA) {
        this(searchBackPressureClusterRCA, null, null, null, null);
    }

    /**
     * records issues which the policy cares about and discards others
     *
     * @param issue an issue with the application
     */
    private void record(HotResourceSummary summary) {
        if (HEAP_SEARCHBP_SHARD_SIGNALS.contains(summary.getResource())) {
            searchBackPressureIssue =
                    new SearchBackPressureShardIssue(
                            summary, searchBackPressureShardAlarmMonitorMap);
            searchBackPressureIssue.recordIssueBySummaryType(summary);
        }

        if (HEAP_SEARCHBP_TASK_SIGNALS.contains(summary.getResource())) {
            searchBackPressureIssue =
                    new SearchBackPressureSearchTaskIssue(
                            summary, searchBackPressureTaskAlarmMonitorMap);
            searchBackPressureIssue.recordIssueBySummaryType(summary);
        }
    }

    /** gathers and records all issues observed in the application */
    private void recordIssues() {
        LOG.debug("SearchBackPressurePolicy#recordIssues()");

        if (searchBackPressureClusterRCA.getFlowUnits().isEmpty()) {
            LOG.debug("No flow units in searchBackPressureClusterRCA");
            return;
        }

        for (ResourceFlowUnit<HotClusterSummary> flowUnit :
                searchBackPressureClusterRCA.getFlowUnits()) {
            if (!flowUnit.hasResourceSummary()) {
                continue;
            }

            HotClusterSummary clusterSummary = flowUnit.getSummary();
            clusterSummary.getHotNodeSummaryList().stream()
                    .flatMap((nodeSummary) -> nodeSummary.getHotResourceSummaryList().stream())
                    .forEach((resourceSummary) -> record(resourceSummary));
        }
    }

    public boolean isShardHeapThresholdTooSmall() {
        return !searchBackPressureShardHeapIncreaseAlarm.isHealthy();
    }

    public boolean isShardHeapThresholdTooLarge() {
        return !searchBackPressureShardHeapDecreaseAlarm.isHealthy();
    }

    public boolean isTaskHeapThresholdTooSmall() {
        return !searchBackPressureTaskHeapIncreaseAlarm.isHealthy();
    }

    public boolean isTaskHeapThresholdTooLarge() {
        return !searchBackPressureTaskHeapDecreaseAlarm.isHealthy();
    }

    // create alarm monitor from config
    public SearchBpActionsAlarmMonitor createAlarmMonitor(Path persistenceBasePath) {
        LOG.debug(
                "createAlarmMonitor with hour window: {}, bucket size: {}, hour threshold: {}, stepsize: {}",
                policyConfig.getHourMonitorWindowSizeMinutes(),
                policyConfig.getHourMonitorBucketSizeMinutes(),
                policyConfig.getHourBreachThreshold(),
                policyConfig.getSearchbpHeapStepsizeInPercentage());
        BucketizedSlidingWindowConfig hourMonitorConfig =
                new BucketizedSlidingWindowConfig(
                        policyConfig.getHourMonitorWindowSizeMinutes(),
                        policyConfig.getHourMonitorBucketSizeMinutes(),
                        TimeUnit.MINUTES,
                        persistenceBasePath);

        // TODO: Check whether we need a persistence path to write our data
        return new SearchBpActionsAlarmMonitor(
                policyConfig.getHourBreachThreshold(), null, hourMonitorConfig);
    }

    // initalize all alarm monitors
    public void initialize() {
        // initialize shard level alarm for resounce unit that suggests to increase jvm threshold
        searchBackPressureShardHeapIncreaseAlarm =
                initializeAlarmMonitor(searchBackPressureShardHeapIncreaseAlarm);

        // initialize shard level alarm for resounce unit that suggests to decrease jvm threshold
        searchBackPressureShardHeapDecreaseAlarm =
                initializeAlarmMonitor(searchBackPressureShardHeapDecreaseAlarm);

        // initialize task level alarm for resounce unit that suggests to increase jvm threshold
        searchBackPressureTaskHeapIncreaseAlarm =
                initializeAlarmMonitor(searchBackPressureTaskHeapIncreaseAlarm);

        // initialize task level alarm for resounce unit that suggests to decrease jvm threhsold
        searchBackPressureTaskHeapDecreaseAlarm =
                initializeAlarmMonitor(searchBackPressureTaskHeapDecreaseAlarm);

        initializeAlarmMonitorMap();
    }

    private SearchBpActionsAlarmMonitor initializeAlarmMonitor(
            SearchBpActionsAlarmMonitor alarmMonitor) {
        if (alarmMonitor == null) {
            return createAlarmMonitor(SEARCHBP_DATA_FILE_PATH);
        } else {
            return alarmMonitor;
        }
    }

    private void initializeAlarmMonitorMap() {
        // add shard level monitors to shardAlarmMonitorMap
        searchBackPressureShardAlarmMonitorMap = new HashMap<String, SearchBpActionsAlarmMonitor>();
        searchBackPressureShardAlarmMonitorMap.put(
                SearchbpShardAlarmMonitorMapKeys.SHARD_HEAP_INCREASE_ALARM.toString(),
                searchBackPressureShardHeapIncreaseAlarm);
        searchBackPressureShardAlarmMonitorMap.put(
                SearchbpShardAlarmMonitorMapKeys.SHARD_HEAP_DECREASE_ALARM.toString(),
                searchBackPressureShardHeapDecreaseAlarm);

        // add task level monitors to taskAlarmMonitorMap
        searchBackPressureTaskAlarmMonitorMap = new HashMap<String, SearchBpActionsAlarmMonitor>();
        searchBackPressureTaskAlarmMonitorMap.put(
                SearchbpTaskAlarmMonitorMapKeys.TASK_HEAP_INCREASE_ALARM.toString(),
                searchBackPressureTaskHeapIncreaseAlarm);
        searchBackPressureTaskAlarmMonitorMap.put(
                SearchbpTaskAlarmMonitorMapKeys.TASK_HEAP_DECREASE_ALARM.toString(),
                searchBackPressureTaskHeapDecreaseAlarm);
    }

    @Override
    public List<Action> evaluate() {
        List<Action> actions = new ArrayList<>();
        if (rcaConf == null || appContext == null) {
            LOG.error("rca conf/app context is null, return empty action list");
            return actions;
        }

        policyConfig = rcaConf.getDeciderConfig().getSearchBackPressurePolicyConfig();
        if (!policyConfig.isEnabled()) {
            LOG.debug("SearchBackPressurePolicy is disabled");
            return actions;
        }

        initialize();

        recordIssues();

        checkShardAlarms(actions);
        checkTaskAlarms(actions);

        // print current size of the actions
        LOG.debug("SearchBackPressurePolicy#evaluate() action size: {}", actions.size());

        return actions;
    }

    private void checkShardAlarms(List<Action> actions) {
        if (isShardHeapThresholdTooSmall()) {
            LOG.debug("isShardHeapThresholdTooSmall action Added");
            actions.add(
                    new SearchBackPressureAction(
                            appContext,
                            true,
                            DEAFULT_COOLOFF_PERIOD_IN_MILLIS,
                            HEAP_THRESHOLD_STR,
                            SearchBackPressureAction.SearchbpDimension.SHARD,
                            SearchBackPressureAction.SearchbpThresholdActionDirection.INCREASE,
                            policyConfig.getSearchbpHeapStepsizeInPercentage()));
        } else if (isShardHeapThresholdTooLarge()) {
            LOG.debug("isShardHeapThresholdTooLarge action Added");
            actions.add(
                    new SearchBackPressureAction(
                            appContext,
                            true,
                            DEAFULT_COOLOFF_PERIOD_IN_MILLIS,
                            HEAP_THRESHOLD_STR,
                            SearchBackPressureAction.SearchbpDimension.SHARD,
                            SearchBackPressureAction.SearchbpThresholdActionDirection.DECREASE,
                            policyConfig.getSearchbpHeapStepsizeInPercentage()));
        }
    }

    private void checkTaskAlarms(List<Action> actions) {
        if (isTaskHeapThresholdTooSmall()) {
            LOG.debug("isTaskHeapThresholdTooSmall action Added");
            actions.add(
                    new SearchBackPressureAction(
                            appContext,
                            true,
                            DEAFULT_COOLOFF_PERIOD_IN_MILLIS,
                            HEAP_THRESHOLD_STR,
                            SearchBackPressureAction.SearchbpDimension.TASK,
                            SearchBackPressureAction.SearchbpThresholdActionDirection.INCREASE,
                            policyConfig.getSearchbpHeapStepsizeInPercentage()));
        } else if (isTaskHeapThresholdTooLarge()) {
            LOG.debug("isTaskHeapThresholdTooLarge action Added");
            actions.add(
                    new SearchBackPressureAction(
                            appContext,
                            true,
                            DEAFULT_COOLOFF_PERIOD_IN_MILLIS,
                            HEAP_THRESHOLD_STR,
                            SearchBackPressureAction.SearchbpDimension.TASK,
                            SearchBackPressureAction.SearchbpThresholdActionDirection.DECREASE,
                            policyConfig.getSearchbpHeapStepsizeInPercentage()));
        }
    }

    public void setAppContext(AppContext appContext) {
        this.appContext = appContext;
    }

    public void setRcaConf(final RcaConf rcaConf) {
        this.rcaConf = rcaConf;
    }
}
