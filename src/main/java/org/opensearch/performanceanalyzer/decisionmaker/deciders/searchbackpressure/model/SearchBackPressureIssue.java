/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.searchbackpressure.model;


import java.util.HashMap;
import java.util.Map;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.searchbackpressure.SearchBpActionsAlarmMonitor;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;

/*
 * SearchBackPressureIssue is the interface for all types of SearchBackPressure Issue (e.g. issue caused by overflow of shard-level heap usage)
 */
public abstract class SearchBackPressureIssue {
    public HotResourceSummary hotResourceSummary;
    public Map<String, SearchBpActionsAlarmMonitor> actionsAlarmMonitorMap;

    // constructor
    SearchBackPressureIssue(
            HotResourceSummary hotResourceSummary,
            HashMap<String, SearchBpActionsAlarmMonitor> actionsAlarmMonitorMap) {
        this.hotResourceSummary = hotResourceSummary;
        this.actionsAlarmMonitorMap = actionsAlarmMonitorMap;
    }

    public abstract void recordIssueBySummaryType(HotResourceSummary hotResourceSummary);
}
