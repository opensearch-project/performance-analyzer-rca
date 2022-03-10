/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.collectors;


import org.junit.Assert;
import org.junit.Before;

public class GCInfoCollectorTest extends AbstractCollectorTest {
    @Before
    public void setup() {
        setUut(new GCInfoCollector());
    }

    @Override
    public void validateMetric(String metric) throws Exception {
        GCInfoCollector.GCInfo info = mapper.readValue(metric, GCInfoCollector.GCInfo.class);
        // TODO implement further validation of the MetricStatus
        Assert.assertFalse(info.getCollectorName().isEmpty());
        Assert.assertFalse(info.getMemoryPool().isEmpty());
    }
}
