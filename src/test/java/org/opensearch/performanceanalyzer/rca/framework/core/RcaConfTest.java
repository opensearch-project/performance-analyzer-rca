/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.core;


import java.nio.file.Paths;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.CachePriorityOrderConfig;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.DeciderConfig;
import org.opensearch.performanceanalyzer.decisionmaker.deciders.configs.WorkLoadTypeConfig;
import org.opensearch.performanceanalyzer.rca.configs.FieldDataCacheRcaConfig;
import org.opensearch.performanceanalyzer.rca.configs.HighHeapUsageOldGenRcaConfig;
import org.opensearch.performanceanalyzer.rca.configs.HighHeapUsageYoungGenRcaConfig;
import org.opensearch.performanceanalyzer.rca.configs.HotNodeClusterRcaConfig;
import org.opensearch.performanceanalyzer.rca.configs.ShardRequestCacheRcaConfig;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;

public class RcaConfTest {

    private RcaConf rcaConf;

    @Before
    public void init() {
        String rcaConfPath =
                Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca_elected_cluster_manager.conf")
                        .toString();
        rcaConf = new RcaConf(rcaConfPath);
    }

    @Test
    public void testReadRcaConfig() {
        Integer topK =
                rcaConf.readRcaConfig(
                        HighHeapUsageOldGenRcaConfig.CONFIG_NAME,
                        HighHeapUsageOldGenRcaConfig.RCA_CONF_KEY_CONSTANTS.TOP_K,
                        HighHeapUsageOldGenRcaConfig.DEFAULT_TOP_K,
                        Integer.class);
        Assert.assertNotNull(topK);
        Assert.assertEquals(HighHeapUsageOldGenRcaConfig.DEFAULT_TOP_K, topK.intValue());

        Integer promotionRateThreshold =
                rcaConf.readRcaConfig(
                        HighHeapUsageYoungGenRcaConfig.CONFIG_NAME,
                        HighHeapUsageYoungGenRcaConfig.RCA_CONF_KEY_CONSTANTS.PROMOTION_RATE_THRES,
                        HighHeapUsageYoungGenRcaConfig
                                .DEFAULT_PROMOTION_RATE_THRESHOLD_IN_MB_PER_SEC,
                        Integer.class);
        Assert.assertNotNull(promotionRateThreshold);
        Assert.assertEquals(
                HighHeapUsageYoungGenRcaConfig.DEFAULT_PROMOTION_RATE_THRESHOLD_IN_MB_PER_SEC,
                promotionRateThreshold.intValue());

        Double unbalancedResourceThreshold =
                rcaConf.readRcaConfig(
                        HotNodeClusterRcaConfig.CONFIG_NAME,
                        HotNodeClusterRcaConfig.RCA_CONF_KEY_CONSTANTS.UNBALANCED_RESOURCE_THRES,
                        HotNodeClusterRcaConfig.DEFAULT_UNBALANCED_RESOURCE_THRES,
                        Double.class);
        Assert.assertNotNull(unbalancedResourceThreshold);
        Assert.assertEquals(
                HotNodeClusterRcaConfig.DEFAULT_UNBALANCED_RESOURCE_THRES,
                unbalancedResourceThreshold,
                0.01);

        Integer val =
                rcaConf.readRcaConfig(
                        HotNodeClusterRcaConfig.CONFIG_NAME,
                        HotNodeClusterRcaConfig.RCA_CONF_KEY_CONSTANTS.UNBALANCED_RESOURCE_THRES,
                        0,
                        Integer.class);
        Assert.assertNotNull(val);
        Assert.assertEquals(0, val.intValue());

        val =
                rcaConf.readRcaConfig(
                        HighHeapUsageOldGenRcaConfig.CONFIG_NAME, "test", 0, Integer.class);
        Assert.assertNotNull(val);
        Assert.assertEquals(0, val.intValue());

        Integer fieldDataTimePeriod =
                rcaConf.readRcaConfig(
                        FieldDataCacheRcaConfig.CONFIG_NAME,
                        FieldDataCacheRcaConfig.RCA_CONF_KEY_CONSTANTS
                                .FIELD_DATA_COLLECTOR_TIME_PERIOD_IN_SEC,
                        FieldDataCacheRcaConfig.DEFAULT_FIELD_DATA_COLLECTOR_TIME_PERIOD_IN_SEC,
                        Integer.class);
        Assert.assertNotNull(fieldDataTimePeriod);
        Assert.assertEquals(10, fieldDataTimePeriod.intValue());

        Integer shardRequestTimePeriod =
                rcaConf.readRcaConfig(
                        ShardRequestCacheRcaConfig.CONFIG_NAME,
                        ShardRequestCacheRcaConfig.RCA_CONF_KEY_CONSTANTS
                                .SHARD_REQUEST_COLLECTOR_TIME_PERIOD_IN_SEC,
                        ShardRequestCacheRcaConfig
                                .DEFAULT_SHARD_REQUEST_COLLECTOR_TIME_PERIOD_IN_SEC,
                        Integer.class);
        Assert.assertNotNull(shardRequestTimePeriod);
        Assert.assertEquals(10, shardRequestTimePeriod.intValue());
    }

    @Test
    public void testValidateRcaConfig() {
        Integer defaultValue1 =
                rcaConf.readRcaConfig(
                        FieldDataCacheRcaConfig.CONFIG_NAME,
                        FieldDataCacheRcaConfig.RCA_CONF_KEY_CONSTANTS
                                .FIELD_DATA_COLLECTOR_TIME_PERIOD_IN_SEC,
                        0,
                        s -> s < 1,
                        Integer.class);
        Assert.assertNotNull(defaultValue1);
        Assert.assertEquals(0, defaultValue1.intValue());
        Integer defaultValue =
                rcaConf.readRcaConfig(
                        ShardRequestCacheRcaConfig.CONFIG_NAME,
                        ShardRequestCacheRcaConfig.RCA_CONF_KEY_CONSTANTS
                                .SHARD_REQUEST_COLLECTOR_TIME_PERIOD_IN_SEC,
                        0,
                        s -> s < 1,
                        Integer.class);
        Assert.assertNotNull(defaultValue);
        Assert.assertEquals(0, defaultValue.intValue());
    }

    @Test
    public void testReadDeciderConfig() {
        DeciderConfig configObj = new DeciderConfig(rcaConf);
        Assert.assertNotNull(configObj.getCachePriorityOrderConfig());
        Assert.assertNotNull(configObj.getWorkLoadTypeConfig());
        CachePriorityOrderConfig cachePriorityOrderConfig = configObj.getCachePriorityOrderConfig();
        Assert.assertEquals(
                Arrays.asList(
                        "test-fielddata-cache",
                        "test-shard-request-cache",
                        "test-query-cache",
                        "test-bitset-filter-cache"),
                cachePriorityOrderConfig.getPriorityOrder());
        WorkLoadTypeConfig workLoadTypeConfig = configObj.getWorkLoadTypeConfig();
        Assert.assertFalse(workLoadTypeConfig.preferSearch());
        Assert.assertTrue(workLoadTypeConfig.preferIngest());
    }
}
