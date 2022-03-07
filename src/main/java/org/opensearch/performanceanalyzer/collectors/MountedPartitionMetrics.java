/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.collectors;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensearch.performanceanalyzer.metrics.AllMetrics.DevicePartitionDimension;
import org.opensearch.performanceanalyzer.metrics.AllMetrics.DevicePartitionValue;

public class MountedPartitionMetrics extends MetricStatus {
    private String mountPoint;
    private String devicePartition;
    private long totalSpace;
    private long freeSpace;
    private long usableFreeSpace;

    public MountedPartitionMetrics() {}

    public MountedPartitionMetrics(
            String devicePartition,
            String mountPoint,
            long totalSpace,
            long freeSpace,
            long usableFreeSpace) {
        this.devicePartition = devicePartition;
        this.mountPoint = mountPoint;
        this.totalSpace = totalSpace;
        this.freeSpace = freeSpace;
        this.usableFreeSpace = usableFreeSpace;
    }

    @JsonProperty(DevicePartitionDimension.Constants.MOUNT_POINT_VALUE)
    public String getMountPoint() {
        return mountPoint;
    }

    @JsonProperty(DevicePartitionDimension.Constants.DEVICE_PARTITION_VALUE)
    public String getDevicePartition() {
        return devicePartition;
    }

    @JsonProperty(DevicePartitionValue.Constants.TOTAL_SPACE_VALUE)
    public long getTotalSpace() {
        return totalSpace;
    }

    @JsonProperty(DevicePartitionValue.Constants.FREE_SPACE_VALUE)
    public long getFreeSpace() {
        return freeSpace;
    }

    @JsonProperty(DevicePartitionValue.Constants.USABLE_FREE_SPACE_VALUE)
    public long getUsableFreeSpace() {
        return usableFreeSpace;
    }
}
