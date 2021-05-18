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


import java.util.Set;
import org.opensearch.performanceanalyzer.hwnet.Disks;
import org.opensearch.performanceanalyzer.hwnet.MountedPartitions;
import org.opensearch.performanceanalyzer.hwnet.NetworkE2E;
import org.opensearch.performanceanalyzer.hwnet.NetworkInterface;
import org.opensearch.performanceanalyzer.metrics_generator.CPUPagingActivityGenerator;
import org.opensearch.performanceanalyzer.metrics_generator.DiskIOMetricsGenerator;
import org.opensearch.performanceanalyzer.metrics_generator.DiskMetricsGenerator;
import org.opensearch.performanceanalyzer.metrics_generator.IPMetricsGenerator;
import org.opensearch.performanceanalyzer.metrics_generator.MountedPartitionMetricsGenerator;
import org.opensearch.performanceanalyzer.metrics_generator.OSMetricsGenerator;
import org.opensearch.performanceanalyzer.metrics_generator.SchedMetricsGenerator;
import org.opensearch.performanceanalyzer.metrics_generator.TCPMetricsGenerator;
import org.opensearch.performanceanalyzer.os.OSGlobals;
import org.opensearch.performanceanalyzer.os.ThreadCPU;
import org.opensearch.performanceanalyzer.os.ThreadDiskIO;
import org.opensearch.performanceanalyzer.os.ThreadSched;

public class LinuxOSMetricsGenerator implements OSMetricsGenerator {

    private static OSMetricsGenerator osMetricsGenerator;

    static {
        osMetricsGenerator = new LinuxOSMetricsGenerator();
    }

    public static OSMetricsGenerator getInstance() {

        return osMetricsGenerator;
    }

    @Override
    public String getPid() {

        return OSGlobals.getPid();
    }

    @Override
    public CPUPagingActivityGenerator getPagingActivityGenerator() {

        return ThreadCPU.INSTANCE.getCPUPagingActivity();
    }

    @Override
    public Set<String> getAllThreadIds() {
        return ThreadCPU.INSTANCE.getCPUPagingActivity().getAllThreadIds();
    }

    @Override
    public DiskIOMetricsGenerator getDiskIOMetricsGenerator() {

        return ThreadDiskIO.getIOUtilization();
    }

    @Override
    public SchedMetricsGenerator getSchedMetricsGenerator() {

        return ThreadSched.INSTANCE.getSchedLatency();
    }

    @Override
    public TCPMetricsGenerator getTCPMetricsGenerator() {

        return NetworkE2E.getTCPMetricsHandler();
    }

    @Override
    public IPMetricsGenerator getIPMetricsGenerator() {

        return NetworkInterface.getLinuxIPMetricsGenerator();
    }

    @Override
    public DiskMetricsGenerator getDiskMetricsGenerator() {

        return Disks.getDiskMetricsHandler();
    }

    @Override
    public MountedPartitionMetricsGenerator getMountedPartitionMetricsGenerator() {
        return MountedPartitions.getLinuxMountedPartitionMetricsGenerator();
    }
}
