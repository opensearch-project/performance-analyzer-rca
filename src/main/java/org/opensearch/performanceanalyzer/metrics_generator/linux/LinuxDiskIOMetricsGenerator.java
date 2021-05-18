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
import org.opensearch.performanceanalyzer.metrics_generator.DiskIOMetricsGenerator;
import org.opensearch.performanceanalyzer.os.ThreadDiskIO;

public class LinuxDiskIOMetricsGenerator implements DiskIOMetricsGenerator {

    private Map<String, ThreadDiskIO.IOMetrics> diskIOMetricsMap;

    public LinuxDiskIOMetricsGenerator() {
        diskIOMetricsMap = new HashMap<>();
    }

    @Override
    public double getAvgReadThroughputBps(final String threadId) {

        return diskIOMetricsMap.get(threadId).avgReadThroughputBps;
    }

    @Override
    public double getAvgReadSyscallRate(final String threadId) {

        return diskIOMetricsMap.get(threadId).avgReadSyscallRate;
    }

    @Override
    public double getAvgWriteThroughputBps(final String threadId) {

        return diskIOMetricsMap.get(threadId).avgWriteThroughputBps;
    }

    @Override
    public double getAvgWriteSyscallRate(final String threadId) {

        return diskIOMetricsMap.get(threadId).avgWriteSyscallRate;
    }

    @Override
    public double getAvgTotalThroughputBps(final String threadId) {

        return diskIOMetricsMap.get(threadId).avgTotalThroughputBps;
    }

    @Override
    public double getAvgTotalSyscallRate(final String threadId) {

        return diskIOMetricsMap.get(threadId).avgTotalSyscallRate;
    }

    @Override
    public boolean hasDiskIOMetrics(final String threadId) {

        return diskIOMetricsMap.containsKey(threadId);
    }

    @Override
    public void addSample() {
        ThreadDiskIO.addSample();
    }

    public void setDiskIOMetrics(final String threadId, final ThreadDiskIO.IOMetrics ioMetrics) {
        diskIOMetricsMap.put(threadId, ioMetrics);
    }
}
