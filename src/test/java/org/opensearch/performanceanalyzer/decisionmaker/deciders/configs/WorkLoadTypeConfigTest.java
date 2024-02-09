/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.configs;

import org.junit.Assert;
import org.junit.Test;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

public class WorkLoadTypeConfigTest {

    @Test
    public void testConfigOverrides() throws Exception {
        final String configStr =
                "{"
                        + "\"decider-config-settings\": { "
                        + "\"workload-type\": { "
                        + "\"prefer-ingest\": true, "
                        + "\"prefer-search\": false "
                        + "} "
                        + "} "
                        + "} ";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        DeciderConfig deciderConfig = new DeciderConfig(conf);
        WorkLoadTypeConfig workLoadTypeConfig = deciderConfig.getWorkLoadTypeConfig();
        Assert.assertNotNull(workLoadTypeConfig);
        Assert.assertFalse(workLoadTypeConfig.preferSearch());
        Assert.assertTrue(workLoadTypeConfig.preferIngest());
    }

    @Test
    public void testDefaults() throws Exception {
        final String configStr = "{}";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        DeciderConfig deciderConfig = new DeciderConfig(conf);
        WorkLoadTypeConfig workLoadTypeConfig = deciderConfig.getWorkLoadTypeConfig();
        Assert.assertNotNull(workLoadTypeConfig);
        Assert.assertEquals(
                WorkLoadTypeConfig.DEFAULT_PREFER_INGEST, workLoadTypeConfig.preferSearch());
        Assert.assertEquals(
                WorkLoadTypeConfig.DEFAULT_PREFER_SEARCH, workLoadTypeConfig.preferIngest());
    }
}
