/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.jvm;


import org.junit.Assert;
import org.junit.Test;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.DeciderConfig;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

public class LevelTwoActionBuilderConfigTest {

    @Test
    public void testConfigOverrides() throws Exception {
        final String configStr =
                "{"
                        + "\"decider-config-settings\": { "
                        + "\"old-gen-decision-policy-config\": { "
                        + "\"level-two-config\": { "
                        + "\"fielddata-cache-step-size\": 5, "
                        + "\"shard-request-cache-step-size\": 6, "
                        + "\"write-queue-step-size\": 5, "
                        + "\"search-queue-step-size\": 6 "
                        + "} "
                        + "} "
                        + "} "
                        + "} ";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        DeciderConfig deciderConfig = new DeciderConfig(conf);
        LevelTwoActionBuilderConfig actionBuilderConfig =
                deciderConfig.getOldGenDecisionPolicyConfig().levelTwoActionBuilderConfig();
        Assert.assertNotNull(actionBuilderConfig);
        Assert.assertEquals(5, actionBuilderConfig.fieldDataCacheStepSize());
        Assert.assertEquals(6, actionBuilderConfig.shardRequestCacheStepSize());
        Assert.assertEquals(5, actionBuilderConfig.writeQueueStepSize());
        Assert.assertEquals(6, actionBuilderConfig.searchQueueStepSize());
    }

    @Test
    public void testDefaults() throws Exception {
        final String configStr = "{}";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        DeciderConfig deciderConfig = new DeciderConfig(conf);
        LevelTwoActionBuilderConfig actionBuilderConfig =
                deciderConfig.getOldGenDecisionPolicyConfig().levelTwoActionBuilderConfig();
        Assert.assertNotNull(actionBuilderConfig);
        Assert.assertEquals(
                LevelTwoActionBuilderConfig.DEFAULT_FIELD_DATA_CACHE_STEP_SIZE,
                actionBuilderConfig.fieldDataCacheStepSize());
        Assert.assertEquals(
                LevelTwoActionBuilderConfig.DEFAULT_SHARD_REQUEST_CACHE_STEP_SIZE,
                actionBuilderConfig.shardRequestCacheStepSize());
        Assert.assertEquals(
                LevelTwoActionBuilderConfig.DEFAULT_WRITE_QUEUE_STEP_SIZE,
                actionBuilderConfig.writeQueueStepSize());
        Assert.assertEquals(
                LevelTwoActionBuilderConfig.DEFAULT_SEARCH_QUEUE_STEP_SIZE,
                actionBuilderConfig.searchQueueStepSize());
    }
}
