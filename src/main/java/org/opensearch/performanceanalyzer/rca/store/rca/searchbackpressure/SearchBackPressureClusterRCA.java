/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.searchbackpressure;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.BaseClusterRca;

public class SearchBackPressureClusterRCA extends BaseClusterRca {

    public static final String RCA_TABLE_NAME = SearchBackPressureClusterRCA.class.getSimpleName();
    private static final Logger LOG = LogManager.getLogger(SearchBackPressureClusterRCA.class);

    public <R extends Rca<ResourceFlowUnit<HotNodeSummary>>> SearchBackPressureClusterRCA(
            final int rcaPeriod, final R SearchBackPressureRCA) {
        super(rcaPeriod, SearchBackPressureRCA);
        LOG.info("SearchBackPressureClusterRCA enabeld.");
    }
}
