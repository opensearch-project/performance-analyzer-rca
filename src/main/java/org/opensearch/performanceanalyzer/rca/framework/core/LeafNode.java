/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.core;

public abstract class LeafNode<T extends GenericFlowUnit> extends Node<T> implements Gatherable<T> {
    private boolean addedToFlowField;

    public LeafNode(int level, long evaluationIntervalSeconds) {
        super(level, evaluationIntervalSeconds);
        Stats stats = Stats.getInstance();
        stats.incrementLeafNodesCount();
    }

    public boolean isAddedToFlowField() {
        return addedToFlowField;
    }

    public void setAddedToFlowField() {
        this.addedToFlowField = true;
    }
}
