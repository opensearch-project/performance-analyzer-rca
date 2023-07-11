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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.decisionmaker.actions.Action;
import org.opensearch.performanceanalyzer.decisionmaker.actions.SearchBackPressureAction;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.DecisionPolicy;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.searchbackpressure.SearchBackPressurePolicyConfig;
import org.opensearch.performanceanalyzer.grpc.Resource;
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

    // TO DO READ THE cool off period from the config file of the action
    private static final long DEAFULT_COOLOFF_PERIOD_IN_MILLIS = 60L * 60L * 1000L;

    private static final Path SEARCHBP_DATA_FILE_PATH =
            Paths.get(RcaConsts.CONFIG_DIR_PATH, "SearchBackPressurePolicy_heap");

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
        LOG.info("SearchBackPressurePolicy#SearchBackPressurePolicy() initialize");
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
        LOG.info("SearchBackPressurePolicy#record()");
        if (HEAP_SEARCHBP_SIGNALS.contains(issue.getResource())) {
            LOG.info(
                    "Recording issue in searchBackPressureHeapAlarm since Resource Searchbp Shard or Task appears");
            searchBackPressureHeapAlarm.recordIssue();
        }
    }

    /** gathers and records all issues observed in the application */
    private void recordIssues() {
        LOG.info("SearchBackPressurePolicy#recordIssues()");

        if (searchBackPressureClusterRCA.getFlowUnits().isEmpty()) {
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

    /* TO DO: Change the logic of heapThresholdIsTooSmall */
    public boolean heapThresholdIsTooSmall() {
        return !searchBackPressureHeapAlarm.isHealthy();
    }

    /* TO DO: Change the logic of heapThresholdIsTooLarge */
    public boolean heapThresholdIsTooLarge() {
        return !searchBackPressureHeapAlarm.isHealthy();
    }

    // create alarm monitor from config
    public SearchBpActionsAlarmMonitor createAlarmMonitor(Path persistenceBasePath) {
        // BucketizedSlidingWindowConfig hourMonitorConfig =
        //         new BucketizedSlidingWindowConfig(
        //                 policyConfig.getDayMonitorWindowSizeMinutes(),
        //                 policyConfig.getDayMonitorBucketSizeMinutes(),
        //                 TimeUnit.MINUTES,
        //                 persistenceBasePath);

        return new SearchBpActionsAlarmMonitor();
    }

    // initalize all alarm monitors
    public void initialize() {
        LOG.info("Initializing alarms with dummy path");
        if (searchBackPressureHeapAlarm == null) {
            searchBackPressureHeapAlarm = createAlarmMonitor(SEARCHBP_DATA_FILE_PATH);
        }
    }

    @Override
    public List<Action> evaluate() {
        LOG.info("---------------evaluate() called");
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
                "searchBackPressureHeapAlarm#hour breach threshold is {}",
                searchBackPressureHeapAlarm.getHourBreachThreshold());

        recordIssues();

        if (heapThresholdIsTooSmall()) {
            LOG.info(
                    "SearchBackPressurePolicy#evaluate() heap usage need to be autotuned. Action Added!");
            // suggest the downstream cls to modify heap usgae threshold
            actions.add(
                    new SearchBackPressureAction(
                            appContext,
                            true,
                            DEAFULT_COOLOFF_PERIOD_IN_MILLIS,
                            "heap_usage",
                            75.0,
                            70));
        }

        // else if (youngGenerationIsTooSmall()) {
        //     LOG.debug("The young generation is too small!");
        //     int newRatio = computeIncrease(getCurrentRatio());
        //     if (newRatio >= 1) {
        //         LOG.debug("Adding new JvmGenAction with ratio {}", newRatio);
        //         actions.add(new JvmGenAction(appContext, newRatio, COOLOFF_PERIOD_IN_MILLIS,
        // true));
        //     }
        // }

        return actions;
    }

    public void setAppContext(AppContext appContext) {
        this.appContext = appContext;
    }

    public void setRcaConf(final RcaConf rcaConf) {
        this.rcaConf = rcaConf;
    }
}
