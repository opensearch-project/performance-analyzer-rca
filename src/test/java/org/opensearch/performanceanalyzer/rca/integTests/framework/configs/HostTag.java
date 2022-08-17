/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.framework.configs;

public enum HostTag {
    ELECTED_CLUSTER_MANAGER,

    // The STANDBY_CLUSTER_MANAGER_X tags are only used in dedicated cluster_manager node clusters.
    STANDBY_CLUSTER_MANAGER_0,
    STANDBY_CLUSTER_1,
    DATA_0,
    DATA_1,
}
