/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
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
