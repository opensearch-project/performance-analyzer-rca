/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.jvm;

import org.junit.Assert;
import org.junit.Test;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.DeciderConfig;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

public class LevelOneActionBuilderConfigTest {

    @Test
    public void testConfigOverrides() throws Exception {
        final String configStr =
                "{"
                        + "\"decider-config-settings\": { "
                        + "\"old-gen-decision-policy-config\": { "
                        + "\"level-one-config\": { "
                        + "\"fielddata-cache-step-size\": 3, "
                        + "\"shard-request-cache-step-size\": 3 "
                        + "} "
                        + "} "
                        + "} "
                        + "} ";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        DeciderConfig deciderConfig = new DeciderConfig(conf);
        LevelOneActionBuilderConfig levelOneActionBuilderConfig =
                deciderConfig.getOldGenDecisionPolicyConfig().levelOneActionBuilderConfig();
        Assert.assertNotNull(levelOneActionBuilderConfig);
        Assert.assertEquals(3, levelOneActionBuilderConfig.fieldDataCacheStepSize());
        Assert.assertEquals(3, levelOneActionBuilderConfig.shardRequestCacheStepSize());
    }

    @Test
    public void testDefaults() throws Exception {
        final String configStr = "{}";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        DeciderConfig deciderConfig = new DeciderConfig(conf);
        LevelOneActionBuilderConfig levelOneActionBuilderConfig =
                deciderConfig.getOldGenDecisionPolicyConfig().levelOneActionBuilderConfig();
        Assert.assertNotNull(levelOneActionBuilderConfig);
        Assert.assertEquals(
                LevelOneActionBuilderConfig.DEFAULT_FIELD_DATA_CACHE_STEP_SIZE,
                levelOneActionBuilderConfig.fieldDataCacheStepSize());
        Assert.assertEquals(
                LevelOneActionBuilderConfig.DEFAULT_SHARD_REQUEST_CACHE_STEP_SIZE,
                levelOneActionBuilderConfig.shardRequestCacheStepSize());
    }
}
