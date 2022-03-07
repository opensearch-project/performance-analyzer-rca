/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.summaries.bucket;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.performanceanalyzer.grpc.ResourceEnum;

public class BasicBucketCalculatorTest {
    BasicBucketCalculator uut;

    @Before
    public void setup() {
        uut = new BasicBucketCalculator(10.0, 40.0, 70.0);
    }

    @Test
    public void testCompute() {
        Assert.assertEquals(UsageBucket.UNDER_UTILIZED, uut.compute(ResourceEnum.CPU, -10.0));
        Assert.assertEquals(UsageBucket.UNDER_UTILIZED, uut.compute(ResourceEnum.CPU, 10.0));
        Assert.assertEquals(UsageBucket.HEALTHY_WITH_BUFFER, uut.compute(ResourceEnum.CPU, 40.0));
        Assert.assertEquals(UsageBucket.HEALTHY, uut.compute(ResourceEnum.CPU, 70.0));
        Assert.assertEquals(UsageBucket.UNHEALTHY, uut.compute(ResourceEnum.CPU, 70.1));
        Assert.assertEquals(UsageBucket.UNHEALTHY, uut.compute(ResourceEnum.CPU, 10000));
    }

    @Test
    public void testInvalidBucketCalculator() {
        try {
            uut = new BasicBucketCalculator(10.0, 5.0, 10.0);
            Assert.fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
}
