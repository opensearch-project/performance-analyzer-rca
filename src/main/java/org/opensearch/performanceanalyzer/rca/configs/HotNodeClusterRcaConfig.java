/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.configs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

/** config object to store rca config settings for HotNodeClusterRca */
public class HotNodeClusterRcaConfig {
    private static final Logger LOG = LogManager.getLogger(HotNodeClusterRcaConfig.class);
    public static final String CONFIG_NAME = "hot-node-cluster-rca";
    private Double unbalancedResourceThreshold;
    private Double resourceUsageLowerBoundThreshold;
    public static final double DEFAULT_UNBALANCED_RESOURCE_THRES = 0.3;
    public static final double DEFAULT_RESOURCE_USAGE_LOWER_BOUND_THRES = 0.1;

    public HotNodeClusterRcaConfig(final RcaConf rcaConf) {
        unbalancedResourceThreshold =
                rcaConf.readRcaConfig(
                        CONFIG_NAME,
                        RCA_CONF_KEY_CONSTANTS.UNBALANCED_RESOURCE_THRES,
                        DEFAULT_UNBALANCED_RESOURCE_THRES,
                        (s) -> (s > 0),
                        Double.class);
        resourceUsageLowerBoundThreshold =
                rcaConf.readRcaConfig(
                        CONFIG_NAME,
                        RCA_CONF_KEY_CONSTANTS.RESOURCE_USAGE_LOWER_BOUND_THRES,
                        DEFAULT_RESOURCE_USAGE_LOWER_BOUND_THRES,
                        (s) -> (s > 0),
                        Double.class);
    }

    public double getUnbalancedResourceThreshold() {
        return unbalancedResourceThreshold;
    }

    public double getResourceUsageLowerBoundThreshold() {
        return resourceUsageLowerBoundThreshold;
    }

    public static class RCA_CONF_KEY_CONSTANTS {
        public static final String UNBALANCED_RESOURCE_THRES = "unbalanced-resource-percentage";
        public static final String RESOURCE_USAGE_LOWER_BOUND_THRES =
                "resource-usage-lower-bound-percentage";
    }
}
