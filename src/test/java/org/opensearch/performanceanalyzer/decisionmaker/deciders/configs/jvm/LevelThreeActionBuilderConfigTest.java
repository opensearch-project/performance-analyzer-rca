/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.jvm;

import org.junit.Assert;
import org.junit.Test;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.DeciderConfig;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

public class LevelThreeActionBuilderConfigTest {

    @Test
    public void testConfigOverrides() throws Exception {
        final String configStr =
                "{"
                        + "\"decider-config-settings\": { "
                        + "\"old-gen-decision-policy-config\": { "
                        + "\"level-three-config\": { "
                        + "\"fielddata-cache-step-size\": 5, "
                        + "\"shard-request-cache-step-size\": 6, "
                        + "\"write-queue-step-size\": 7, "
                        + "\"search-queue-step-size\": 8 "
                        + "} "
                        + "} "
                        + "} "
                        + "} ";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        DeciderConfig deciderConfig = new DeciderConfig(conf);
        LevelThreeActionBuilderConfig actionBuilderConfig =
                deciderConfig.getOldGenDecisionPolicyConfig().levelThreeActionBuilderConfig();
        Assert.assertNotNull(actionBuilderConfig);
        Assert.assertEquals(7, actionBuilderConfig.writeQueueStepSize());
        Assert.assertEquals(8, actionBuilderConfig.searchQueueStepSize());
    }

    @Test
    public void testDefaults() throws Exception {
        final String configStr = "{}";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        DeciderConfig deciderConfig = new DeciderConfig(conf);
        LevelThreeActionBuilderConfig actionBuilderConfig =
                deciderConfig.getOldGenDecisionPolicyConfig().levelThreeActionBuilderConfig();
        Assert.assertNotNull(actionBuilderConfig);
        Assert.assertEquals(
                LevelThreeActionBuilderConfig.DEFAULT_WRITE_QUEUE_STEP_SIZE,
                actionBuilderConfig.writeQueueStepSize());
        Assert.assertEquals(
                LevelThreeActionBuilderConfig.DEFAULT_SEARCH_QUEUE_STEP_SIZE,
                actionBuilderConfig.searchQueueStepSize());
    }
}
