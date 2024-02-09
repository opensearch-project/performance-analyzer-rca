/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.core;

import org.opensearch.performanceanalyzer.rca.framework.api.Resources;

public abstract class GenericContext {
    private final Resources.State state;

    public GenericContext(Resources.State state) {
        this.state = state;
    }

    public Resources.State getState() {
        return this.state;
    }

    public boolean isUnhealthy() {
        return this.state == Resources.State.UNHEALTHY || this.state == Resources.State.CONTENDED;
    }

    public boolean isHealthy() {
        return this.state == Resources.State.HEALTHY;
    }

    public boolean isUnknown() {
        return this.state == Resources.State.UNKNOWN;
    }

    @Override
    public String toString() {
        return this.state.toString();
    }
}
