/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.cluster;


import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;

public class ShardRequestCacheClusterRca extends BaseClusterRca {
    public static final String RCA_TABLE_NAME = ShardRequestCacheClusterRca.class.getSimpleName();

    public <R extends Rca<ResourceFlowUnit<HotNodeSummary>>> ShardRequestCacheClusterRca(
            final int rcaPeriod, final R hotNodeRca) {
        super(rcaPeriod, hotNodeRca);
    }
}
