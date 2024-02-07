/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;

import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Net_PacketRate4 extends Metric {
    public Net_PacketRate4(long evaluationIntervalSeconds) {
        super(AllMetrics.IPValue.NET_PACKET_RATE4.name(), evaluationIntervalSeconds);
    }
}
