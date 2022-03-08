/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.actions.configs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;

public class CacheActionConfigTest {

    @Test
    public void testConfigOverrides() throws Exception {
        final String configStr =
                "{"
                        + "\"action-config-settings\": { "
                        + "\"cache-settings\": { "
                        + "\"fielddata\": { "
                        + "\"upper-bound\": 0.75, "
                        + "\"lower-bound\": 0.55 "
                        + "}, "
                        + "\"shard-request\": { "
                        + "\"upper-bound\": 0.042, "
                        + "\"lower-bound\": 0.013 "
                        + "} "
                        + "} "
                        + "} "
                        + "}";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        CacheActionConfig cacheActionConfig = new CacheActionConfig(conf);
        assertEquals(
                0.75,
                cacheActionConfig.getThresholdConfig(ResourceEnum.FIELD_DATA_CACHE).upperBound(),
                0.00001);
        assertEquals(
                0.55,
                cacheActionConfig.getThresholdConfig(ResourceEnum.FIELD_DATA_CACHE).lowerBound(),
                0.00001);
        assertEquals(
                0.042,
                cacheActionConfig.getThresholdConfig(ResourceEnum.SHARD_REQUEST_CACHE).upperBound(),
                0.00001);
        assertEquals(
                0.013,
                cacheActionConfig.getThresholdConfig(ResourceEnum.SHARD_REQUEST_CACHE).lowerBound(),
                0.00001);
    }

    @Test
    public void testDefaults() throws Exception {
        final String configStr = "{}";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        CacheActionConfig cacheActionConfig = new CacheActionConfig(conf);
        assertEquals(
                CacheActionConfig.DEFAULT_FIELDDATA_CACHE_UPPER_BOUND,
                cacheActionConfig.getThresholdConfig(ResourceEnum.FIELD_DATA_CACHE).upperBound(),
                0.00001);
        assertEquals(
                CacheActionConfig.DEFAULT_FIELDDATA_CACHE_LOWER_BOUND,
                cacheActionConfig.getThresholdConfig(ResourceEnum.FIELD_DATA_CACHE).lowerBound(),
                0.00001);
        assertEquals(
                CacheActionConfig.DEFAULT_SHARD_REQUEST_CACHE_UPPER_BOUND,
                cacheActionConfig.getThresholdConfig(ResourceEnum.SHARD_REQUEST_CACHE).upperBound(),
                0.00001);
        assertEquals(
                CacheActionConfig.DEFAULT_SHARD_REQUEST_CACHE_LOWER_BOUND,
                cacheActionConfig.getThresholdConfig(ResourceEnum.SHARD_REQUEST_CACHE).lowerBound(),
                0.00001);
    }

    @Test
    public void testInvalidConfigValues() throws Exception {
        final String configStr =
                "{"
                        + "\"action-config-settings\": { "
                        + "\"cache-settings\": { "
                        + "\"fielddata\": { "
                        + "\"upper-bound\": 0.0, "
                        + "\"lower-bound\": 0.0 "
                        + "}, "
                        + "\"shard-request\": { "
                        + "\"upper-bound\": 0.0, "
                        + "\"lower-bound\": 0.0 "
                        + "} "
                        + "} "
                        + "}"
                        + "}";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);

        // Invalid values in config, should resolve back to defaults
        CacheActionConfig cacheActionConfig = new CacheActionConfig(conf);
        assertEquals(
                CacheActionConfig.DEFAULT_FIELDDATA_CACHE_UPPER_BOUND,
                cacheActionConfig.getThresholdConfig(ResourceEnum.FIELD_DATA_CACHE).upperBound(),
                0.00001);
        assertEquals(
                CacheActionConfig.DEFAULT_FIELDDATA_CACHE_LOWER_BOUND,
                cacheActionConfig.getThresholdConfig(ResourceEnum.FIELD_DATA_CACHE).lowerBound(),
                0.00001);
        assertEquals(
                CacheActionConfig.DEFAULT_SHARD_REQUEST_CACHE_UPPER_BOUND,
                cacheActionConfig.getThresholdConfig(ResourceEnum.SHARD_REQUEST_CACHE).upperBound(),
                0.00001);
        assertEquals(
                CacheActionConfig.DEFAULT_SHARD_REQUEST_CACHE_LOWER_BOUND,
                cacheActionConfig.getThresholdConfig(ResourceEnum.SHARD_REQUEST_CACHE).lowerBound(),
                0.00001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCacheType() throws Exception {
        final String configStr = "{}";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        CacheActionConfig cacheActionConfig = new CacheActionConfig(conf);
        cacheActionConfig.getThresholdConfig(ResourceEnum.SEARCH_THREADPOOL).upperBound();
    }

    @Test
    public void testGetStepSize() throws Exception {
        String configStr =
                "{"
                        + "\"action-config-settings\": { "
                        + "\"cache-settings\": { "
                        + "\"fielddata\": { "
                        + "\"upper-bound\": 0.8, "
                        + "\"lower-bound\": 0.2 "
                        + "}, "
                        + "\"shard-request\": { "
                        + "\"upper-bound\": 0.08, "
                        + "\"lower-bound\": 0.01 "
                        + "} "
                        + "} "
                        + "} "
                        + "}";
        RcaConf conf = new RcaConf();
        conf.readConfigFromString(configStr);
        CacheActionConfig cacheActionConfig = new CacheActionConfig(conf);
        assertEquals(0.03, cacheActionConfig.getStepSize(ResourceEnum.FIELD_DATA_CACHE), 0.0001);
        assertEquals(
                0.0035, cacheActionConfig.getStepSize(ResourceEnum.SHARD_REQUEST_CACHE), 0.0001);
        configStr =
                "{"
                        + "\"action-config-settings\": { "
                        + "\"cache-settings\": { "
                        + "\"total-step-count\": 5,"
                        + "\"fielddata\": { "
                        + "\"upper-bound\": 0.8, "
                        + "\"lower-bound\": 0.2 "
                        + "}, "
                        + "\"shard-request\": { "
                        + "\"upper-bound\": 0.08, "
                        + "\"lower-bound\": 0.01 "
                        + "} "
                        + "} "
                        + "} "
                        + "}";
        conf.readConfigFromString(configStr);
        cacheActionConfig = new CacheActionConfig(conf);
        assertEquals(0.12, cacheActionConfig.getStepSize(ResourceEnum.FIELD_DATA_CACHE), 0.0001);
        assertEquals(
                0.014, cacheActionConfig.getStepSize(ResourceEnum.SHARD_REQUEST_CACHE), 0.0001);
    }
}
