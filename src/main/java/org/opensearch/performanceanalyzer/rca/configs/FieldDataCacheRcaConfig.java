/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.configs;

import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

/** config object to store rca config settings for FieldDataCacheRca */
public class FieldDataCacheRcaConfig {
    public static final String CONFIG_NAME = "field-data-cache-rca-config";

    private Double fieldDataCacheSizeThreshold;
    private Integer fieldDataCollectorTimePeriodInSec;

    // Field data cache size threshold is 80%
    public static final double DEFAULT_FIELD_DATA_CACHE_SIZE_THRESHOLD = 0.8;
    // Metrics like eviction, hits are collected every 300 sec in field data cache rca
    public static final int DEFAULT_FIELD_DATA_COLLECTOR_TIME_PERIOD_IN_SEC = 300;

    public FieldDataCacheRcaConfig(final RcaConf rcaConf) {
        fieldDataCacheSizeThreshold =
                rcaConf.readRcaConfig(
                        CONFIG_NAME,
                        RCA_CONF_KEY_CONSTANTS.FIELD_DATA_CACHE_SIZE_THRESHOLD,
                        DEFAULT_FIELD_DATA_CACHE_SIZE_THRESHOLD,
                        (s) -> (s > 0),
                        Double.class);
        fieldDataCollectorTimePeriodInSec =
                rcaConf.readRcaConfig(
                        CONFIG_NAME,
                        RCA_CONF_KEY_CONSTANTS.FIELD_DATA_COLLECTOR_TIME_PERIOD_IN_SEC,
                        DEFAULT_FIELD_DATA_COLLECTOR_TIME_PERIOD_IN_SEC,
                        (s) -> (s > 0),
                        Integer.class);
    }

    public double getFieldDataCacheSizeThreshold() {
        return fieldDataCacheSizeThreshold;
    }

    public int getFieldDataCollectorTimePeriodInSec() {
        return fieldDataCollectorTimePeriodInSec;
    }

    public static class RCA_CONF_KEY_CONSTANTS {
        public static final String FIELD_DATA_CACHE_SIZE_THRESHOLD =
                "field-data-cache-size-threshold";
        public static final String FIELD_DATA_COLLECTOR_TIME_PERIOD_IN_SEC =
                "field-data-collector-time-period-in-sec";
    }
}
