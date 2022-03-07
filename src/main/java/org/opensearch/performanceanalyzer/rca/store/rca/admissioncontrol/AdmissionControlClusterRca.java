/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.admissioncontrol;


import org.opensearch.performanceanalyzer.rca.framework.api.Rca;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.store.rca.cluster.BaseClusterRca;

public class AdmissionControlClusterRca extends BaseClusterRca {

    public static final String RCA_TABLE_NAME = AdmissionControlClusterRca.class.getSimpleName();

    public <R extends Rca<ResourceFlowUnit<HotNodeSummary>>> AdmissionControlClusterRca(
            final int rcaPeriod, final R admissionControlRca) {
        super(rcaPeriod, admissionControlRca);
    }
}
