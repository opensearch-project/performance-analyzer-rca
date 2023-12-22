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

public class SearchBackPressureShardIssue extends SearchBackPressureIssue {
    private static final Logger LOG = LogManager.getLogger(SearchBackPressureShardIssue.class);

    public SearchBackPressureShardIssue(
            HotResourceSummary hotResourceSummary,
            HashMap<String, SearchBpActionsAlarmMonitor> actionsAlarmMonitorMap) {
        super(hotResourceSummary, actionsAlarmMonitorMap);
    }

    @Override
    public void recordIssueBySummaryType(HotResourceSummary summary) {
        LOG.debug("Recording issue by summary type..... summary: {}", summary);
        if (summary.getMetaData()
                .equalsIgnoreCase(SearchBackPressureRcaConfig.INCREASE_THRESHOLD_BY_JVM_STR)) {
            LOG.debug("recording increase-level issue for shard");
            LOG.debug("size of the HashMap: {}", actionsAlarmMonitorMap.size());
            actionsAlarmMonitorMap
                    .get(SearchbpShardAlarmMonitorMapKeys.SHARD_HEAP_INCREASE_ALARM.toString())
                    .recordIssue();
        }

        // decrease alarm for heap-related threshold
        if (summary.getMetaData()
                .equalsIgnoreCase(SearchBackPressureRcaConfig.DECREASE_THRESHOLD_BY_JVM_STR)) {
            LOG.debug("recording decrease-level issue for shard");
            actionsAlarmMonitorMap
                    .get(SearchbpShardAlarmMonitorMapKeys.SHARD_HEAP_DECREASE_ALARM.toString())
                    .recordIssue();
        }
    }

    public enum SearchbpShardAlarmMonitorMapKeys {
        SHARD_HEAP_INCREASE_ALARM(
                SearchbpShardAlarmMonitorMapKeys.Constants.SHARD_HEAP_DECREASE_ALARM),
        SHARD_HEAP_DECREASE_ALARM(
                SearchbpShardAlarmMonitorMapKeys.Constants.SHARD_HEAP_DECREASE_ALARM);

        private final String value;

        SearchbpShardAlarmMonitorMapKeys(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static class Constants {
            public static final String SHARD_HEAP_INCREASE_ALARM =
                    "searchBackPressureShardHeapIncreaseAlarm";
            public static final String SHARD_HEAP_DECREASE_ALARM =
                    "searchBackPressureShardHeapDecreaseAlarm";
        }
    }
}
