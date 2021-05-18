/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
