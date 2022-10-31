/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.framework.configs;

public enum HostTag {
    ELECTED_MASTER,

    // The STANDBY_MASTER_X tags are only used in dedicated master node clusters.
    STANDBY_MASTER_0,
    STANDBY_MASTER_1,
    DATA_0,
    DATA_1,
}
