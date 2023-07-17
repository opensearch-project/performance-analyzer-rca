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
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.actions.SearchBackPressureAction;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.DecisionPolicy;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.searchbackpressure.SearchBackPressurePolicyConfig;
import org.opensearch.performanceanalyzer.grpc.Resource;
import org.opensearch.performanceanalyzer.rca.configs.SearchBackPressureRcaConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.aggregators.BucketizedSlidingWindowConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
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

    /* TO DO: Specify a path to store SearchBackpressurePolicy_Autotune Stats */

    private AppContext appContext;
    private RcaConf rcaConf;
    private SearchBackPressurePolicyConfig policyConfig;
    private SearchBackPressureClusterRCA searchBackPressureClusterRCA;

    /* Alarm for heap usage */
    static final List<Resource> HEAP_SEARCHBP_SHARD_SIGNALS =
            Lists.newArrayList(SEARCHBACKPRESSURE_SHARD);
    static final List<Resource> HEAP_SEARCHBP_TASK_SIGNALS =
            Lists.newArrayList(SEARCHBACKPRESSURE_TASK);

    /* alarm monitors per threshold */
    // shard-level alarms
    @VisibleForTesting SearchBpActionsAlarmMonitor searchBackPressureShardHeapIncreaseAlarm;
    @VisibleForTesting SearchBpActionsAlarmMonitor searchBackPressureShardHeapDecreaseAlarm;

    // task-level alarms
    @VisibleForTesting SearchBpActionsAlarmMonitor searchBackPressureTaskHeapIncreaseAlarm;
    @VisibleForTesting SearchBpActionsAlarmMonitor searchBackPressureTaskHeapDecreaseAlarm;

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
        LOG.info("SearchBackPressurePolicy#SearchBackPressurePolicy() initialized");
    }

    public SearchBackPressurePolicy(SearchBackPressureClusterRCA searchBackPressureClusterRCA) {
        this(searchBackPressureClusterRCA, null, null, null, null);
    }

    /**
     * records issues which the policy cares about and discards others
     *
     * @param issue an issue with the application
     */
    private void record(HotResourceSummary issue) {
        LOG.info("SearchBackPressurePolicy#record()");
        if (HEAP_SEARCHBP_SHARD_SIGNALS.contains(issue.getResource())) {
            LOG.info("Recording shard-level issue");
            // increase alarm for heap-related threshold (shard-level)
            if (issue.getMetaData() == SearchBackPressureRcaConfig.INCREASE_THRESHOLD_BY_JVM_STR) {
                LOG.info("recording increase-level issue for shard");
                searchBackPressureShardHeapIncreaseAlarm.recordIssue();
            }

            // decrease alarm for heap-related threshold (shard-level)
            if (issue.getMetaData() == SearchBackPressureRcaConfig.DECREASE_THRESHOLD_BY_JVM_STR) {
                LOG.info("recording decrease-level issue for shard");
                searchBackPressureShardHeapDecreaseAlarm.recordIssue();
            }

        } else if (HEAP_SEARCHBP_TASK_SIGNALS.contains(issue.getResource())) {
            LOG.info("Recording Task-Level issue");

            // increase alarm for heap-related threshold (task-level)
            if (issue.getMetaData() == SearchBackPressureRcaConfig.INCREASE_THRESHOLD_BY_JVM_STR) {
                LOG.info("recording increase-level issue for task");
                searchBackPressureTaskHeapIncreaseAlarm.recordIssue();
            }

            // decrease alarm for heap-related threshold (task-level)
            if (issue.getMetaData() == SearchBackPressureRcaConfig.DECREASE_THRESHOLD_BY_JVM_STR) {
                LOG.info("recording decrease-level issue for task");
                searchBackPressureTaskHeapDecreaseAlarm.recordIssue();
            }
        }
    }

    /** gathers and records all issues observed in the application */
    private void recordIssues() {
        LOG.info("SearchBackPressurePolicy#recordIssues()");

        if (searchBackPressureClusterRCA.getFlowUnits().isEmpty()) {
            LOG.info(
                    "SearchBackPressurePolicy#recordIssues() No flow units in searchBackPressureClusterRCA");
            return;
        }
        int test_counter = 0;
        for (ResourceFlowUnit<HotClusterSummary> flowUnit :
                searchBackPressureClusterRCA.getFlowUnits()) {
            if (!flowUnit.hasResourceSummary()) {
                continue;
            }
            // print out the total number of flow units in length
            HotClusterSummary clusterSummary = flowUnit.getSummary();
            for (HotNodeSummary nodeSummary : clusterSummary.getHotNodeSummaryList()) {
                for (HotResourceSummary summary : nodeSummary.getHotResourceSummaryList()) {
                    test_counter += 1;
                    LOG.info(
                            "SearchBackPressurePolicy#recordIssues() Summary test_counter: "
                                    + test_counter);
                    record(summary);
                }
            }
        }
    }

    public boolean shardHeapThresholdIsTooSmall() {
        return !searchBackPressureShardHeapIncreaseAlarm.isHealthy();
    }

    public boolean shardHeapThresholdIsTooLarge() {
        return !searchBackPressureShardHeapDecreaseAlarm.isHealthy();
    }

    public boolean taskHeapThresholdIsTooSmall() {
        return !searchBackPressureTaskHeapIncreaseAlarm.isHealthy();
    }

    public boolean taskHeapThresholdIsTooLarge() {
        return !searchBackPressureTaskHeapDecreaseAlarm.isHealthy();
    }

    // create alarm monitor from config
    public SearchBpActionsAlarmMonitor createAlarmMonitor(Path persistenceBasePath) {
        // LOG the policyConfig.getHourMonitorWindowSizeMinutes() BuketSize and dahy breanch
        // threhsold
        LOG.info(
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
        return new SearchBpActionsAlarmMonitor(policyConfig.getHourBreachThreshold());
    }

    // initalize all alarm monitors
    public void initialize() {
        LOG.info("Initializing alarms with dummy path");
        // initialize shard level alarm for resounce unit that suggests to increase jvm threshold
        if (searchBackPressureShardHeapIncreaseAlarm == null) {
            searchBackPressureShardHeapIncreaseAlarm = createAlarmMonitor(SEARCHBP_DATA_FILE_PATH);
        }

        // initialize shard level alarm for resounce unit that suggests to decrease jvm threshold
        if (searchBackPressureShardHeapDecreaseAlarm == null) {
            searchBackPressureShardHeapDecreaseAlarm = createAlarmMonitor(SEARCHBP_DATA_FILE_PATH);
        }

        // initialize task level alarm for resounce unit that suggests to increase jvm threshold
        if (searchBackPressureTaskHeapIncreaseAlarm == null) {
            searchBackPressureTaskHeapIncreaseAlarm = createAlarmMonitor(SEARCHBP_DATA_FILE_PATH);
        }

        // initialize task level alarm for resounce unit that suggests to decrease jvm threhsold
        if (searchBackPressureTaskHeapDecreaseAlarm == null) {
            searchBackPressureTaskHeapDecreaseAlarm = createAlarmMonitor(SEARCHBP_DATA_FILE_PATH);
        }
    }

    @Override
    public List<Action> evaluate() {
        LOG.info("Evaluate() of SearchBackpressurePolicy started");
        List<Action> actions = new ArrayList<>();
        if (rcaConf == null || appContext == null) {
            LOG.error("rca conf/app context is null, return empty action list");
            return actions;
        }

        policyConfig = rcaConf.getDeciderConfig().getSearchBackPressurePolicyConfig();
        if (!policyConfig.isEnabled()) {
            LOG.info("SearchBackPressurePolicy is disabled");
            return actions;
        }

        initialize();
        LOG.info(
                "searchBackPressureShardHeapIncreaseAlarm#hour breach threshold is {}",
                searchBackPressureShardHeapIncreaseAlarm.getHourBreachThreshold());

        recordIssues();

        if (shardHeapThresholdIsTooSmall()) {
            LOG.info("shardHeapThresholdIsTooSmall action Added!");
            actions.add(
                    new SearchBackPressureAction(
                            appContext,
                            true,
                            DEAFULT_COOLOFF_PERIOD_IN_MILLIS,
                            HEAP_THRESHOLD_STR,
                            SHARD_DIMENSION_STR,
                            SearchBackPressureAction.SearchbpThresholdActionDirection.INCREASE
                                    .toString(),
                            policyConfig.getSearchbpHeapStepsizeInPercentage()));
        } else if (shardHeapThresholdIsTooLarge()) {
            LOG.info("shardHeapThresholdIsTooLarge action Added!");
            actions.add(
                    new SearchBackPressureAction(
                            appContext,
                            true,
                            DEAFULT_COOLOFF_PERIOD_IN_MILLIS,
                            HEAP_THRESHOLD_STR,
                            SHARD_DIMENSION_STR,
                            SearchBackPressureAction.SearchbpThresholdActionDirection.DECREASE
                                    .toString(),
                            policyConfig.getSearchbpHeapStepsizeInPercentage()));
        } else if (taskHeapThresholdIsTooSmall()) {
            LOG.info("taskHeapThresholdIsTooSmall action Added!");
            actions.add(
                    new SearchBackPressureAction(
                            appContext,
                            true,
                            DEAFULT_COOLOFF_PERIOD_IN_MILLIS,
                            HEAP_THRESHOLD_STR,
                            TASK_DIMENSION_STR,
                            SearchBackPressureAction.SearchbpThresholdActionDirection.INCREASE
                                    .toString(),
                            policyConfig.getSearchbpHeapStepsizeInPercentage()));
        } else if (taskHeapThresholdIsTooLarge()) {
            LOG.info("taskHeapThresholdIsTooLarge action Added!");
            actions.add(
                    new SearchBackPressureAction(
                            appContext,
                            true,
                            DEAFULT_COOLOFF_PERIOD_IN_MILLIS,
                            HEAP_THRESHOLD_STR,
                            TASK_DIMENSION_STR,
                            SearchBackPressureAction.SearchbpThresholdActionDirection.DECREASE
                                    .toString(),
                            policyConfig.getSearchbpHeapStepsizeInPercentage()));
        }

        return actions;
    }

    public void setAppContext(AppContext appContext) {
        this.appContext = appContext;
    }

    public void setRcaConf(final RcaConf rcaConf) {
        this.rcaConf = rcaConf;
    }
}
