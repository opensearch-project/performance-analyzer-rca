/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.contexts;


import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericContext;

public class SymptomContext extends GenericContext {
    public SymptomContext(Resources.State state) {
        super(state);
    }

    public static SymptomContext generic() {
        return new SymptomContext(Resources.State.UNKNOWN);
    }
}
