/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.collectors;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.TCPDimension;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics.TCPValue;

public class TCPStatus extends MetricStatus {

    private String dest;

    private int numFlows;

    private double txQ;

    private double rxQ;

    private double curLost;

    private double sndCWND;

    // make this field private so that Jackson uses getter method name
    private double ssThresh;

    public TCPStatus() {}

    public TCPStatus(
            String dest,
            int numFlows,
            double txQ,
            double rxQ,
            double curLost,
            double sndCWND,
            double sSThresh) {
        super();
        this.dest = dest;
        this.numFlows = numFlows;
        this.txQ = txQ;
        this.rxQ = rxQ;
        this.curLost = curLost;
        this.sndCWND = sndCWND;
        this.ssThresh = sSThresh;
    }

    @JsonProperty(TCPDimension.Constants.DEST_VALUE)
    public String getDest() {
        return dest;
    }

    @JsonProperty(TCPValue.Constants.NUM_FLOWS_VALUE)
    public int getNumFlows() {
        return numFlows;
    }

    @JsonProperty(TCPValue.Constants.TXQ_VALUE)
    public double getTxQ() {
        return txQ;
    }

    @JsonProperty(TCPValue.Constants.RXQ_VALUE)
    public double getRxQ() {
        return rxQ;
    }

    @JsonProperty(TCPValue.Constants.CUR_LOST_VALUE)
    public double getCurLost() {
        return curLost;
    }

    @JsonProperty(TCPValue.Constants.SEND_CWND_VALUE)
    public double getSndCWND() {
        return sndCWND;
    }

    @JsonProperty(TCPValue.Constants.SSTHRESH_VALUE)
    public double getSsThresh() {
        return ssThresh;
    }
}
