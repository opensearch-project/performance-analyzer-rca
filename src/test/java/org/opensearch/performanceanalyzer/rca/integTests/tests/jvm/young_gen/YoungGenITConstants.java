/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.tests.jvm.young_gen;


import org.opensearch.performanceanalyzer.rca.integTests.framework.configs.Consts;

public final class YoungGenITConstants {
    public static final String RCA_CONF_PATH = Consts.TEST_RESOURCES_DIR + "young_gen/rca.conf";
    public static final String RCA_MASTER_CONF_PATH =
            Consts.TEST_RESOURCES_DIR + "young_gen/rca_master.conf";

    public static final String RCA_HIGH_THRESHOLD_CONF_PATH =
            Consts.TEST_RESOURCES_DIR + "young_gen/rca_high_threshold.conf";
    public static final String RCA_MASTER_HIGH_THRESHOLD_CONF_PATH =
            Consts.TEST_RESOURCES_DIR + "young_gen/rca_master_high_threshold.conf";
}
