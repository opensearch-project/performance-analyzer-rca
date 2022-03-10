/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metrics_generator.linux;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opensearch.performanceanalyzer.metrics_generator.CPUPagingActivityGenerator;
import org.opensearch.performanceanalyzer.os.ThreadCPU;

public class LinuxCPUPagingActivityGenerator implements CPUPagingActivityGenerator {

    private Map<String, Double> cpu;
    private Map<String, Double[]> pagingActivities;

    public LinuxCPUPagingActivityGenerator() {
        cpu = new HashMap<>();
        pagingActivities = new HashMap<>();
    }

    @Override
    public double getCPUUtilization(final String threadId) {

        return cpu.getOrDefault(threadId, 0.0);
    }

    @Override
    public double getMajorFault(final String threadId) {

        return pagingActivities.get(threadId)[0];
    }

    @Override
    public double getMinorFault(final String threadId) {

        return pagingActivities.get(threadId)[1];
    }

    @Override
    public double getResidentSetSize(final String threadId) {

        return pagingActivities.get(threadId)[2];
    }

    @Override
    public boolean hasPagingActivity(final String threadId) {

        return pagingActivities.containsKey(threadId);
    }

    @Override
    public void addSample() {

        cpu.clear();
        pagingActivities.clear();
        ThreadCPU.INSTANCE.addSample();
    }

    public void setCPUUtilization(final String threadId, final Double cpuUtilization) {

        cpu.put(threadId, cpuUtilization);
    }

    public Set<String> getAllThreadIds() {

        return cpu.keySet();
    }

    public void setPagingActivities(final String threadId, final Double[] activityes) {
        pagingActivities.put(threadId, activityes);
    }
}
