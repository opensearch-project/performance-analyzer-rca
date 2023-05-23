/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.collectors;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.DiskDimension;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.DiskValue;

public class DiskMetrics extends MetricStatus {
    public String name;

    public double utilization; // fraction, 0-1

    public double await; // ms

    public double serviceRate; // MBps

    public DiskMetrics(String name, double utilization, double await, double serviceRate) {
        super();
        this.name = name;
        this.utilization = utilization;
        this.await = await;
        this.serviceRate = serviceRate;
    }

    public DiskMetrics() {
        super();
    }

    @JsonProperty(DiskDimension.Constants.NAME_VALUE)
    public String getName() {
        return name;
    }

    @JsonProperty(DiskValue.Constants.UTIL_VALUE)
    public double getUtilization() {
        return utilization;
    }

    @JsonProperty(DiskValue.Constants.WAIT_VALUE)
    public double getAwait() {
        return await;
    }

    @JsonProperty(DiskValue.Constants.SRATE_VALUE)
    public double getServiceRate() {
        return serviceRate;
    }
}
