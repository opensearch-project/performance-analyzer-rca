/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.jvmsizing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.api.contexts.ResourceContext;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;

public class LargeHeapClusterRcaTest {
    @Mock private OldGenContendedRca mockOldGenContendedRca;

    @Mock private AppContext mockAppContext;

    private LargeHeapClusterRca testRca;
    private final List<InstanceDetails> healthyInstances =
            Collections.singletonList(getInstanceDetail("node1", "1.2.3.4"));
    private final List<InstanceDetails> unhealthyInstances =
            Collections.singletonList(getInstanceDetail("node2", "1.2.3.5"));

    @Before
    public void setup() throws Exception {
        initMocks(this);
        setupAppContext();
        this.testRca = new LargeHeapClusterRca(mockOldGenContendedRca);
        testRca.setAppContext(mockAppContext);
    }

    @Test
    public void testEmptyFlowUnits() {
        when(mockOldGenContendedRca.getFlowUnits()).thenReturn(Collections.emptyList());
        ResourceFlowUnit<HotClusterSummary> flowUnit = testRca.operate();

        assertTrue(flowUnit.isEmpty());
    }

    @Test
    public void testSomeUnhealthyNodes() {
        ResourceFlowUnit<HotNodeSummary> oldGenFlowUnit =
                new ResourceFlowUnit<>(
                        System.currentTimeMillis(),
                        new ResourceContext(Resources.State.CONTENDED),
                        new HotNodeSummary(
                                unhealthyInstances.get(0).getInstanceId(),
                                unhealthyInstances.get(0).getInstanceIp()));
        when(mockOldGenContendedRca.getFlowUnits())
                .thenReturn(Collections.singletonList(oldGenFlowUnit));

        ResourceFlowUnit<HotClusterSummary> flowUnit = testRca.operate();
        assertEquals(1, flowUnit.getSummary().getNumOfUnhealthyNodes());

        HotClusterSummary summary = flowUnit.getSummary();
        assertEquals(1, summary.getHotNodeSummaryList().size());
        assertEquals(
                unhealthyInstances.get(0).getInstanceId(),
                summary.getHotNodeSummaryList().get(0).getNodeID());
        assertEquals(
                unhealthyInstances.get(0).getInstanceIp(),
                summary.getHotNodeSummaryList().get(0).getHostAddress());
    }

    private InstanceDetails getInstanceDetail(final String nodeId, final String hostAddress) {
        return new InstanceDetails(
                new InstanceDetails.Id(nodeId), new InstanceDetails.Ip(hostAddress), 0);
    }

    private void setupAppContext() {
        List<InstanceDetails> allInstances = new ArrayList<>(healthyInstances);
        allInstances.addAll(unhealthyInstances);
        when(mockAppContext.getAllClusterInstances()).thenReturn(allInstances);
    }
}
