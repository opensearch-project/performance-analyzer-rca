/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.configs;


import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

public class QueueRejectionRcaConfig {
    private Integer rejectionTimePeriodInSeconds;
    public static final int DEFAULT_REJECTION_TIME_PERIOD_IN_SECONDS = 300;
    public static final String CONFIG_NAME = "queue-rejection-rca";

    public QueueRejectionRcaConfig(final RcaConf rcaConf) {
        rejectionTimePeriodInSeconds =
                rcaConf.readRcaConfig(
                        CONFIG_NAME,
                        RCA_CONF_KEY_CONSTANTS.REJECTION_TIME_PERIOD_IN_SECONDS,
                        DEFAULT_REJECTION_TIME_PERIOD_IN_SECONDS,
                        (s) -> (s > 0),
                        Integer.class);
    }

    public int getRejectionTimePeriodInSeconds() {
        return rejectionTimePeriodInSeconds;
    }

    public static class RCA_CONF_KEY_CONSTANTS {
        public static final String REJECTION_TIME_PERIOD_IN_SECONDS =
                "rejection-time-period-in-seconds";
    }
}
