/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.configs;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

public class HighHeapUsageYoungGenRcaConfig {
    private static final Logger LOG = LogManager.getLogger(HighHeapUsageYoungGenRcaConfig.class);
    public static final String CONFIG_NAME = "high-heap-usage-young-gen-rca";
    private Integer promotionRateThreshold;
    private Integer youngGenGcTimeThreshold;
    private Double garbagePromotionPctThreshold;
    // promotion rate threshold is 500 Mb/s
    public static final int DEFAULT_PROMOTION_RATE_THRESHOLD_IN_MB_PER_SEC = 500;
    // young gc time threshold is 400 ms per second
    public static final int DEFAULT_YOUNG_GEN_GC_TIME_THRESHOLD_IN_MS_PER_SEC = 400;
    public static final double DEFAULT_GARBAGE_PROMOTION_PCT_THRESHOLD = 0.8;

    public HighHeapUsageYoungGenRcaConfig(final RcaConf rcaConf) {
        promotionRateThreshold =
                rcaConf.readRcaConfig(
                        CONFIG_NAME,
                        RCA_CONF_KEY_CONSTANTS.PROMOTION_RATE_THRES,
                        DEFAULT_PROMOTION_RATE_THRESHOLD_IN_MB_PER_SEC,
                        (s) -> (s > 0),
                        Integer.class);
        youngGenGcTimeThreshold =
                rcaConf.readRcaConfig(
                        CONFIG_NAME,
                        RCA_CONF_KEY_CONSTANTS.YOUNG_GEN_GC_TIME_THRES,
                        DEFAULT_YOUNG_GEN_GC_TIME_THRESHOLD_IN_MS_PER_SEC,
                        (s) -> (s > 0),
                        Integer.class);
        garbagePromotionPctThreshold =
                rcaConf.readRcaConfig(
                        CONFIG_NAME,
                        RCA_CONF_KEY_CONSTANTS.GARBAGE_PROMOTION_PCT_THRES,
                        DEFAULT_GARBAGE_PROMOTION_PCT_THRESHOLD,
                        (s) -> s >= 0 && s <= 1,
                        Double.class);
    }

    public int getPromotionRateThreshold() {
        return promotionRateThreshold;
    }

    public int getYoungGenGcTimeThreshold() {
        return youngGenGcTimeThreshold;
    }

    public double getGarbagePromotionPctThreshold() {
        return garbagePromotionPctThreshold;
    }

    public static class RCA_CONF_KEY_CONSTANTS {
        public static final String PROMOTION_RATE_THRES = "promotion-rate-mb-per-second";
        public static final String YOUNG_GEN_GC_TIME_THRES = "young-gen-gc-time-ms-per-second";
        public static final String GARBAGE_PROMOTION_PCT_THRES = "garbage-promotion-pct-threshold";
    }
}
