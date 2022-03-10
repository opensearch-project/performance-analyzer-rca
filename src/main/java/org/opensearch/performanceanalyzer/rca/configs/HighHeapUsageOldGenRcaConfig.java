/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.configs;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

/** config object to store rca config settings in rca.conf */
public class HighHeapUsageOldGenRcaConfig {

    private static final Logger LOG = LogManager.getLogger(HighHeapUsageOldGenRcaConfig.class);
    private Integer topK;
    public static final int DEFAULT_TOP_K = 3;
    public static final String CONFIG_NAME = "high-heap-usage-old-gen-rca";

    public HighHeapUsageOldGenRcaConfig(final RcaConf rcaConf) {
        topK =
                rcaConf.readRcaConfig(
                        CONFIG_NAME,
                        RCA_CONF_KEY_CONSTANTS.TOP_K,
                        DEFAULT_TOP_K,
                        (s) -> (s > 0),
                        Integer.class);
    }

    public int getTopK() {
        return topK;
    }

    public static class RCA_CONF_KEY_CONSTANTS {
        public static final String TOP_K = "top-k";
    }
}
