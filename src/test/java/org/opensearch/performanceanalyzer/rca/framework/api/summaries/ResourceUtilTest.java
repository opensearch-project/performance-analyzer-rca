/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.summaries;


import org.junit.Assert;
import org.junit.Test;
import org.opensearch.performanceanalyzer.grpc.Resource;

public class ResourceUtilTest {

    @Test
    public void testGetResourceTypeName() {
        Assert.assertEquals(
                "old gen", ResourceUtil.getResourceTypeName(ResourceUtil.OLD_GEN_HEAP_USAGE));
        Assert.assertEquals("cpu usage", ResourceUtil.getResourceTypeName(ResourceUtil.CPU_USAGE));
    }

    @Test
    public void testGetResourceTypeUnit() {
        Assert.assertEquals(
                "heap usage(memory usage in percentage)",
                ResourceUtil.getResourceMetricName(ResourceUtil.OLD_GEN_HEAP_USAGE));
        Assert.assertEquals(
                "cpu usage(num of cores)",
                ResourceUtil.getResourceMetricName(ResourceUtil.CPU_USAGE));
    }

    @Test
    public void testBuildResourceType() {
        Resource oldGen = ResourceUtil.buildResource(0, 0);
        Assert.assertEquals(oldGen, ResourceUtil.OLD_GEN_HEAP_USAGE);
        Resource cpuResource = ResourceUtil.buildResource(2, 3);
        Assert.assertEquals(cpuResource, ResourceUtil.CPU_USAGE);
    }
}
