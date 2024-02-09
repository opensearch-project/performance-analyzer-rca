/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.configs;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

public class CachePriorityOrderConfigTest {

    @Test
    public void testConfigOverrides() throws Exception {
        final String configStr =
                "{"
                        + "\"decider-config-settings\": { "
                        + "\"cache-type\": { "
                        + "\"priority-order\": [\"test-fielddata-cache\", "
                        + "\"test-shard-request-cache\", \"test-query-cache\", \"test-bitset-filter-cache\"] "
                        + "} "
                        + "} "
                        + "} ";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        DeciderConfig deciderConfig = new DeciderConfig(conf);
        CachePriorityOrderConfig cachePriorityOrderConfig =
                deciderConfig.getCachePriorityOrderConfig();
        Assert.assertNotNull(cachePriorityOrderConfig);
        Assert.assertEquals(
                Arrays.asList(
                        "test-fielddata-cache",
                        "test-shard-request-cache",
                        "test-query-cache",
                        "test-bitset-filter-cache"),
                cachePriorityOrderConfig.getPriorityOrder());
    }

    @Test
    public void testDefaults() throws Exception {
        final String configStr = "{}";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        DeciderConfig deciderConfig = new DeciderConfig(conf);
        CachePriorityOrderConfig cachePriorityOrderConfig =
                deciderConfig.getCachePriorityOrderConfig();
        Assert.assertNotNull(cachePriorityOrderConfig);
        Assert.assertEquals(
                CachePriorityOrderConfig.DEFAULT_PRIORITY_ORDER,
                cachePriorityOrderConfig.getPriorityOrder());
    }
}
