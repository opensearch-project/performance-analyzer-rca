/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.searchbackpressure.model;

import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.searchbackpressure.SearchBpActionsAlarmMonitor;
import org.opensearch.performanceanalyzer.rca.configs.SearchBackPressureRcaConfig;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;

public class SearchBackPressureSearchTaskIssue extends SearchBackPressureIssue {
    private static final Logger LOG = LogManager.getLogger(SearchBackPressureSearchTaskIssue.class);

    public SearchBackPressureSearchTaskIssue(
            HotResourceSummary hotResourceSummary,
            HashMap<String, SearchBpActionsAlarmMonitor> actionsAlarmMonitorMap) {
        super(hotResourceSummary, actionsAlarmMonitorMap);
    }

    @Override
    public void recordIssueBySummaryType(HotResourceSummary summary) {

        if (summary.getMetaData() == SearchBackPressureRcaConfig.INCREASE_THRESHOLD_BY_JVM_STR) {
            LOG.debug("recording increase-level issue for task");
            actionsAlarmMonitorMap
                    .get(SearchbpTaskAlarmMonitorMapKeys.TASK_HEAP_INCREASE_ALARM.toString())
                    .recordIssue();
        }

        // decrease alarm for heap-related threshold
        if (summary.getMetaData() == SearchBackPressureRcaConfig.DECREASE_THRESHOLD_BY_JVM_STR) {
            LOG.debug("recording decrease-level issue for task");
            actionsAlarmMonitorMap
                    .get(SearchbpTaskAlarmMonitorMapKeys.TASK_HEAP_DECREASE_ALARM.toString())
                    .recordIssue();
        }
    }

    public enum SearchbpTaskAlarmMonitorMapKeys {
        TASK_HEAP_INCREASE_ALARM(
                SearchbpTaskAlarmMonitorMapKeys.Constants.TASK_HEAP_INCREASE_ALARM),
        TASK_HEAP_DECREASE_ALARM(
                SearchbpTaskAlarmMonitorMapKeys.Constants.TASK_HEAP_DECREASE_ALARM);

        private final String value;

        SearchbpTaskAlarmMonitorMapKeys(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static class Constants {
            public static final String TASK_HEAP_INCREASE_ALARM =
                    "searchBackPressureTaskHeapIncreaseAlarm";
            public static final String TASK_HEAP_DECREASE_ALARM =
                    "searchBackPressureTaskHeapDecreaseAlarm";
        }
    }
}
