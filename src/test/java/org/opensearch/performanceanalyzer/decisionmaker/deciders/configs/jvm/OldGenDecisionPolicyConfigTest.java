/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.jvm;


import org.junit.Assert;
import org.junit.Test;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.DeciderConfig;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

public class OldGenDecisionPolicyConfigTest {

    @Test
    public void testConfigOverrides() throws Exception {
        final String configStr =
                "{"
                        + "\"decider-config-settings\": { "
                        + "\"old-gen-decision-policy-config\": { "
                        + "\"queue-bucket-size\": 20, "
                        + "\"old-gen-threshold-level-one\": 0.1, "
                        + "\"old-gen-threshold-level-two\": 0.3, "
                        + "\"old-gen-threshold-level-three\": 0.5 "
                        + "} "
                        + "} "
                        + "} ";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        DeciderConfig deciderConfig = new DeciderConfig(conf);
        OldGenDecisionPolicyConfig oldGenDecisionPolicyConfig =
                deciderConfig.getOldGenDecisionPolicyConfig();
        Assert.assertNotNull(oldGenDecisionPolicyConfig);
        Assert.assertEquals(20, oldGenDecisionPolicyConfig.queueBucketSize());
        Assert.assertEquals(0.1, oldGenDecisionPolicyConfig.oldGenThresholdLevelOne(), 0.01);
        Assert.assertEquals(0.3, oldGenDecisionPolicyConfig.oldGenThresholdLevelTwo(), 0.01);
        Assert.assertEquals(0.5, oldGenDecisionPolicyConfig.oldGenThresholdLevelThree(), 0.01);
    }

    @Test
    public void testInvalidConfig() throws Exception {
        final String configStr =
                "{"
                        + "\"decider-config-settings\": { "
                        + "\"old-gen-decision-policy-config\": { "
                        + "\"queue-bucket-size\": 0, "
                        + "\"old-gen-threshold-level-one\": 0.5, "
                        + "\"old-gen-threshold-level-two\": 0.4, "
                        + "\"old-gen-threshold-level-three\": 0.3 "
                        + "} "
                        + "} "
                        + "} ";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        DeciderConfig deciderConfig = new DeciderConfig(conf);
        OldGenDecisionPolicyConfig oldGenDecisionPolicyConfig =
                deciderConfig.getOldGenDecisionPolicyConfig();
        Assert.assertNotNull(oldGenDecisionPolicyConfig);
        Assert.assertEquals(
                OldGenDecisionPolicyConfig.DEFAULT_QUEUE_BUCKET_SIZE,
                oldGenDecisionPolicyConfig.queueBucketSize());
        Assert.assertEquals(0.5, oldGenDecisionPolicyConfig.oldGenThresholdLevelOne(), 0.01);
        Assert.assertEquals(
                OldGenDecisionPolicyConfig.DEFAULT_OLD_GEN_THRESHOLD_LEVEL_TWO,
                oldGenDecisionPolicyConfig.oldGenThresholdLevelTwo(),
                0.01);
        Assert.assertEquals(
                OldGenDecisionPolicyConfig.DEFAULT_OLD_GEN_THRESHOLD_LEVEL_THREE,
                oldGenDecisionPolicyConfig.oldGenThresholdLevelThree(),
                0.01);
    }

    @Test
    public void testDefaults() throws Exception {
        final String configStr = "{}";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        DeciderConfig deciderConfig = new DeciderConfig(conf);
        OldGenDecisionPolicyConfig oldGenDecisionPolicyConfig =
                deciderConfig.getOldGenDecisionPolicyConfig();
        Assert.assertNotNull(oldGenDecisionPolicyConfig);
        Assert.assertEquals(
                OldGenDecisionPolicyConfig.DEFAULT_QUEUE_BUCKET_SIZE,
                oldGenDecisionPolicyConfig.queueBucketSize());
        Assert.assertEquals(
                OldGenDecisionPolicyConfig.DEFAULT_OLD_GEN_THRESHOLD_LEVEL_ONE,
                oldGenDecisionPolicyConfig.oldGenThresholdLevelOne(),
                0.01);
        Assert.assertEquals(
                OldGenDecisionPolicyConfig.DEFAULT_OLD_GEN_THRESHOLD_LEVEL_TWO,
                oldGenDecisionPolicyConfig.oldGenThresholdLevelTwo(),
                0.01);
        Assert.assertEquals(
                OldGenDecisionPolicyConfig.DEFAULT_OLD_GEN_THRESHOLD_LEVEL_THREE,
                oldGenDecisionPolicyConfig.oldGenThresholdLevelThree(),
                0.01);
    }
}
