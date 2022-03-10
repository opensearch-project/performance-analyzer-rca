/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.model;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ModelTests {

    @Test
    public void testBasicMetric() {
        MetricsModel metricAndDimensions = new MetricsModel();
        assertTrue(MetricsModel.ALL_METRICS.get("pseudocpu") == null);
    }
}
