/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.collectors;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensearch.performanceanalyzer.metrics.AllMetrics.IPDimension;
import org.opensearch.performanceanalyzer.metrics.AllMetrics.IPValue;

// all metrics are per-time-unit
public class NetInterfaceSummary extends MetricStatus {

    public enum Direction {
        in,
        out;
    }

    private Direction direction;
    private double packetRate4;
    private double dropRate4;
    private double packetRate6;
    private double dropRate6;
    private double bps;

    public NetInterfaceSummary() {}

    public NetInterfaceSummary(
            Direction direction,
            double packetRate4,
            double dropRate4,
            double packetRate6,
            double dropRate6,
            double bps) {
        this.direction = direction;
        this.packetRate4 = packetRate4;
        this.dropRate4 = dropRate4;
        this.packetRate6 = packetRate6;
        this.dropRate6 = dropRate6;
        this.bps = bps;
    }

    @JsonProperty(IPDimension.Constants.DIRECTION_VALUE)
    public Direction getDirection() {
        return direction;
    }

    @JsonProperty(IPValue.Constants.PACKET_RATE4_VALUE)
    public double getPacketRate4() {
        return packetRate4;
    }

    @JsonProperty(IPValue.Constants.DROP_RATE4_VALUE)
    public double getDropRate4() {
        return dropRate4;
    }

    @JsonProperty(IPValue.Constants.PACKET_RATE6_VALUE)
    public double getPacketRate6() {
        return packetRate6;
    }

    @JsonProperty(IPValue.Constants.DROP_RATE6_VALUE)
    public double getDropRate6() {
        return dropRate6;
    }

    @JsonProperty(IPValue.Constants.THROUGHPUT_VALUE)
    public double getBps() {
        return bps;
    }
}
