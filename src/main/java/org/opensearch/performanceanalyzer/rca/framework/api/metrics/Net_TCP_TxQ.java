/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.metrics;

import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;

public class Net_TCP_TxQ extends Metric {
    public Net_TCP_TxQ(long evaluationIntervalSeconds) {
        super(AllMetrics.TCPValue.Net_TCP_TXQ.name(), evaluationIntervalSeconds);
    }
}
