/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metrics_generator.linux;


import java.util.HashMap;
import java.util.Map;
import org.opensearch.performanceanalyzer.metrics_generator.SchedMetricsGenerator;
import org.opensearch.performanceanalyzer.os.ThreadSched;

public class LinuxSchedMetricsGenerator implements SchedMetricsGenerator {

    private final Map<String, ThreadSched.SchedMetrics> schedMetricsMap;

    public LinuxSchedMetricsGenerator() {
        schedMetricsMap = new HashMap<>();
    }

    @Override
    public double getAvgRuntime(final String threadId) {

        return schedMetricsMap.get(threadId).avgRuntime;
    }

    @Override
    public double getAvgWaittime(final String threadId) {

        return schedMetricsMap.get(threadId).avgWaittime;
    }

    @Override
    public double getContextSwitchRate(final String threadId) {

        return schedMetricsMap.get(threadId).contextSwitchRate;
    }

    @Override
    public boolean hasSchedMetrics(final String threadId) {

        return schedMetricsMap.containsKey(threadId);
    }

    @Override
    public void addSample() {

        schedMetricsMap.clear();
        ThreadSched.INSTANCE.addSample();
    }

    public void setSchedMetric(final String threadId, final ThreadSched.SchedMetrics schedMetrics) {

        schedMetricsMap.put(threadId, schedMetrics);
    }
}
