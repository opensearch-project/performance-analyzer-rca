/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.framework.configs;

/** Types of clusters supported by the integration test framework. */
public enum ClusterType {
    /** A single node cluster. The only node in the cluster will be tagged as DATA_0 */
    SINGLE_NODE,
    /**
     * A two data-nodes clusters where data-node0 acts as the elected master. node0 will be tagged
     * as ELECTED_MASTER and node1 will be tagged as DATA_0.
     */
    MULTI_NODE_CO_LOCATED_MASTER,

    /**
     * a three dedicated master nodes and two data node cluster. node0 will be tagged as
     * ELECTED_MASTER. node1 will be tagged as STANDBY_MASTER_0. node2 will be tagged as
     * STANDBY_MASTER_1. node3 will be tagged as DATA_0. node4 will be tagged as DATA_1.
     */
    MULTI_NODE_DEDICATED_MASTER
}
