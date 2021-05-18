/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 * Copyright 2019-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
