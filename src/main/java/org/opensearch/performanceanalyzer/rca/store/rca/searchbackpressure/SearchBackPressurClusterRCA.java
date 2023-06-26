/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.searchbackpressure;


import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.BaseClusterRca;

public class SearchBackPressurClusterRCA extends BaseClusterRca {

    public static final String RCA_TABLE_NAME = SearchBackPressurClusterRCA.class.getSimpleName();

    public <R extends Rca<ResourceFlowUnit<HotNodeSummary>>> SearchBackPressurClusterRCA(
            final int rcaPeriod, final R SearchBackPressureRCA) {
        super(rcaPeriod, SearchBackPressureRCA);
    }
}
