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
 * Copyright 2019-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package org.opensearch.performanceanalyzer.rca.store.rca.jvmsizing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotResourceSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.ResourceUtil;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.util.MemInfoParser;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OldGenContendedRca.class, OldGenContendedRcaTest.class, MemInfoParser.class})
@PowerMockIgnore({
    "com.sun.org.apache.xerces.*",
    "javax.xml.*",
    "org.xml.*",
    "javax.management.*",
    "org.w3c.*"
})
public class OldGenContendedRcaTest {
    @Mock private HighOldGenOccupancyRca mockOldGenOccupancyRca;

    @Mock private OldGenReclamationRca mockOldGenReclamationRca;

    @Mock private AppContext mockAppContext;

    private final InstanceDetails currentInstance =
            new InstanceDetails(
                    new InstanceDetails.Id("nodeId"), new InstanceDetails.Ip("1.2.3.4"), 0);
    private OldGenContendedRca testRca;
    private static final long GB_TO_B = 1024L * 1024 * 1024;
    private static final long GB_201_IN_BYTES = 201 * GB_TO_B;
    private static final long GB_32_IN_BYTES = 32 * GB_TO_B;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        PowerMockito.mockStatic(MemInfoParser.class);
        when(mockAppContext.getMyInstanceDetails()).thenReturn(currentInstance);
        when(MemInfoParser.getTotalMemory()).thenReturn(GB_201_IN_BYTES);
        this.testRca = new OldGenContendedRca(mockOldGenOccupancyRca, mockOldGenReclamationRca);
        this.testRca.setAppContext(mockAppContext);
    }

    @Test
    public void testEmptyDependentFlowUnits() {
        when(mockOldGenOccupancyRca.getFlowUnits()).thenReturn(Collections.emptyList());
        when(mockOldGenReclamationRca.getFlowUnits()).thenReturn(Collections.emptyList());

        ResourceFlowUnit<HotNodeSummary> flowUnit = testRca.operate();
        assertTrue(flowUnit.isEmpty());
    }

    @Test
    public void oneDependentRcaUnhealthy() {
        when(mockOldGenOccupancyRca.getFlowUnits())
                .thenReturn(
                        Collections.singletonList(
                                new ResourceFlowUnit<>(
                                        System.currentTimeMillis(),
                                        new ResourceContext(Resources.State.UNHEALTHY),
                                        new HotResourceSummary(
                                                ResourceUtil.OLD_GEN_HEAP_USAGE, 0d, 0d, 0))));
        when(mockOldGenReclamationRca.getFlowUnits())
                .thenReturn(
                        Collections.singletonList(
                                new ResourceFlowUnit<>(
                                        System.currentTimeMillis(),
                                        new ResourceContext(Resources.State.HEALTHY),
                                        new HotResourceSummary(
                                                ResourceUtil.FULL_GC_EFFECTIVENESS, 0d, 0d, 0))));

        ResourceFlowUnit<HotNodeSummary> flowUnit = testRca.operate();
        assertTrue(flowUnit.isEmpty());

        when(mockOldGenOccupancyRca.getFlowUnits())
                .thenReturn(
                        Collections.singletonList(
                                new ResourceFlowUnit<>(
                                        System.currentTimeMillis(),
                                        new ResourceContext(Resources.State.HEALTHY),
                                        new HotResourceSummary(
                                                ResourceUtil.OLD_GEN_HEAP_USAGE, 0d, 0d, 0))));
        when(mockOldGenReclamationRca.getFlowUnits())
                .thenReturn(
                        Collections.singletonList(
                                new ResourceFlowUnit<>(
                                        System.currentTimeMillis(),
                                        new ResourceContext(Resources.State.UNHEALTHY),
                                        new HotResourceSummary(
                                                ResourceUtil.FULL_GC_EFFECTIVENESS, 0d, 0d, 0))));

        assertTrue(testRca.operate().isEmpty());
    }

    @Test
    public void testContendedOldGen() {

        when(mockOldGenOccupancyRca.getFlowUnits())
                .thenReturn(
                        Collections.singletonList(
                                new ResourceFlowUnit<>(
                                        System.currentTimeMillis(),
                                        new ResourceContext(Resources.State.UNHEALTHY),
                                        new HotResourceSummary(
                                                ResourceUtil.OLD_GEN_HEAP_USAGE, 0d, 0d, 0))));
        when(mockOldGenReclamationRca.getFlowUnits())
                .thenReturn(
                        Collections.singletonList(
                                new ResourceFlowUnit<>(
                                        System.currentTimeMillis(),
                                        new ResourceContext(Resources.State.UNHEALTHY),
                                        new HotResourceSummary(
                                                ResourceUtil.FULL_GC_EFFECTIVENESS, 0d, 0d, 0))));

        ResourceFlowUnit<HotNodeSummary> flowUnit = testRca.operate();
        assertFalse(flowUnit.isEmpty());
        Assert.assertEquals(currentInstance.getInstanceId(), flowUnit.getSummary().getNodeID());
        Assert.assertEquals(
                currentInstance.getInstanceIp(), flowUnit.getSummary().getHostAddress());
    }

    @Test
    public void testInsufficientMemory() {
        when(MemInfoParser.getTotalMemory()).thenReturn(GB_32_IN_BYTES);
        // reconstruct the test obj as total memory is read only once.
        this.testRca = new OldGenContendedRca(mockOldGenOccupancyRca, mockOldGenReclamationRca);
        this.testRca.setAppContext(mockAppContext);
        when(mockOldGenOccupancyRca.getFlowUnits())
                .thenReturn(
                        Collections.singletonList(
                                new ResourceFlowUnit<>(
                                        System.currentTimeMillis(),
                                        new ResourceContext(Resources.State.UNHEALTHY),
                                        new HotResourceSummary(
                                                ResourceUtil.OLD_GEN_HEAP_USAGE, 0d, 0d, 0))));
        when(mockOldGenReclamationRca.getFlowUnits())
                .thenReturn(
                        Collections.singletonList(
                                new ResourceFlowUnit<>(
                                        System.currentTimeMillis(),
                                        new ResourceContext(Resources.State.UNHEALTHY),
                                        new HotResourceSummary(
                                                ResourceUtil.FULL_GC_EFFECTIVENESS, 0d, 0d, 0))));

        assertTrue(testRca.operate().isEmpty());
    }
}
