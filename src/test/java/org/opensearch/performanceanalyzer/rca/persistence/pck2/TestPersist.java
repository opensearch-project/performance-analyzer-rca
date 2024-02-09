/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.persistence.pck2;

import org.opensearch.performanceanalyzer.rca.persistence.ValueColumn;

public class TestPersist {
    @ValueColumn int x;

    public TestPersist() {
        this.x = 2;
    }

    public int getX() {
        return x;
    }
}
