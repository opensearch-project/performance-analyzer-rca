/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.messages;

import java.util.List;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericFlowUnit;

public class DataMsg {
    String sourceGraphNode;
    List<String> destinationGraphNodes;
    List<? extends GenericFlowUnit> flowUnits;

    public DataMsg(
            String sourceGraphNode,
            List<String> destinationNodes,
            List<? extends GenericFlowUnit> flowUnits) {
        this.sourceGraphNode = sourceGraphNode;
        this.destinationGraphNodes = destinationNodes;
        this.flowUnits = flowUnits;
    }

    public String getSourceGraphNode() {
        return sourceGraphNode;
    }

    public List<String> getDestinationGraphNodes() {
        return destinationGraphNodes;
    }

    public List<? extends GenericFlowUnit> getFlowUnits() {
        return flowUnits;
    }

    @Override
    public String toString() {
        return String.format("Data::from: '%s', to: %s", sourceGraphNode, destinationGraphNodes);
    }
}
