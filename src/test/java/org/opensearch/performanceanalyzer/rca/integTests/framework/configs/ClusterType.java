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
     * A two data-nodes clusters where data-node0 acts as the elected cluster_manager. node0 will be
     * tagged as ELECTED_CLUSTER_MANAGER and node1 will be tagged as DATA_0.
     */
    MULTI_NODE_CO_LOCATED_CLUSTER_MANAGER,

    /**
     * a three dedicated cluster_manager nodes and two data node cluster. node0 will be tagged as
     * ELECTED_CLUSTER_MANAGER. node1 will be tagged as STANDBY_CLUSTER_MANAGER_0. node2 will be
     * tagged as STANDBY_CLUSTER_MANAGER_1. node3 will be tagged as DATA_0. node4 will be tagged as
     * DATA_1.
     */
    MULTI_NODE_DEDICATED_CLUSTER_MANAGER
}
