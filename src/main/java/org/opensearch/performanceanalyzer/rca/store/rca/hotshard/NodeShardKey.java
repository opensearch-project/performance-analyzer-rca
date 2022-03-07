/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.hotshard;

public class NodeShardKey {
    private final String nodeId;
    private final String shardId;

    public NodeShardKey(String nodeId, String shardId) {
        this.nodeId = nodeId;
        this.shardId = shardId;
    }

    public String getNodeId() {
        return this.nodeId;
    }

    public String getShardId() {
        return this.shardId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof NodeShardKey) {
            NodeShardKey key = (NodeShardKey) obj;
            return nodeId.equals(key.nodeId) && shardId.equals(key.shardId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return nodeId.hashCode() * 31 + shardId.hashCode();
    }

    @Override
    public String toString() {
        return String.join(" ", new String[] {this.nodeId, this.shardId});
    }
}
