/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.samplers;


import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.commons.stats.emitters.ISampler;

public class RcaStateSamplers {

    public static ISampler getRcaEnabledSampler(final AppContext appContext) {
        return new RcaEnabledSampler(appContext);
    }
}
