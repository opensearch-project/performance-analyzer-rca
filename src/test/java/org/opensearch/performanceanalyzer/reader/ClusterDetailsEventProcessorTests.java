/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.opensearch.performanceanalyzer.commons.config.overrides.ConfigOverrides;
import org.opensearch.performanceanalyzer.commons.event_process.Event;
import org.opensearch.performanceanalyzer.config.overrides.ConfigOverridesApplier;

public class ClusterDetailsEventProcessorTests {

    @Mock ConfigOverridesApplier mockOverridesApplier;

    @Captor ArgumentCaptor<String> overridesCaptor;

    private ClusterDetailsEventProcessor testClusterDetailsEventProcessor;
    private ConfigOverrides testOverrides;
    private String nodeId1 = "s7gDCVnCSiuBgHoYLji1gw";
    private String address1 = "10.212.49.140";

    private String nodeId2 = "Zn1QcSUGT--DciD1Em5wRg";
    private String address2 = "10.212.52.241";

    private String disabledDecider = "disabled decider";

    @Before
    public void setup() {
        initMocks(this);

        testOverrides = new ConfigOverrides();
        ConfigOverrides.Overrides disabled = new ConfigOverrides.Overrides();
        disabled.setDeciders(Collections.singletonList(disabledDecider));
        testOverrides.setDisable(disabled);

        testClusterDetailsEventProcessor = new ClusterDetailsEventProcessor(mockOverridesApplier);
    }

    @Test
    public void testProcessEvent() throws Exception {

        boolean isClusterManagerNode1 = true;

        boolean isClusterManagerNode2 = false;

        ClusterDetailsEventProcessor clusterDetailsEventProcessor;
        try {
            ClusterDetailsEventProcessorTestHelper clusterDetailsEventProcessorTestHelper =
                    new ClusterDetailsEventProcessorTestHelper();
            clusterDetailsEventProcessorTestHelper.addNodeDetails(
                    nodeId1, address1, isClusterManagerNode1);
            clusterDetailsEventProcessorTestHelper.addNodeDetails(
                    nodeId2, address2, isClusterManagerNode2);
            clusterDetailsEventProcessor =
                    clusterDetailsEventProcessorTestHelper.generateClusterDetailsEvent();
        } catch (Exception e) {
            Assert.assertTrue("got exception when generating cluster details event", false);
            return;
        }

        List<ClusterDetailsEventProcessor.NodeDetails> nodes =
                clusterDetailsEventProcessor.getNodesDetails();

        assertEquals(nodeId1, nodes.get(0).getId());
        assertEquals(address1, nodes.get(0).getHostAddress());
        assertEquals(isClusterManagerNode1, nodes.get(0).getIsClusterManagerNode());

        assertEquals(nodeId2, nodes.get(1).getId());
        assertEquals(address2, nodes.get(1).getHostAddress());
        assertEquals(isClusterManagerNode2, nodes.get(1).getIsClusterManagerNode());
    }

    @Test
    public void testApplyOverrides() throws Exception {
        ClusterDetailsEventProcessorTestHelper clusterDetailsEventProcessorTestHelper =
                new ClusterDetailsEventProcessorTestHelper();
        clusterDetailsEventProcessorTestHelper.addNodeDetails(nodeId1, address1, true);
        clusterDetailsEventProcessorTestHelper.addNodeDetails(nodeId2, address2, false);

        Event testEvent =
                clusterDetailsEventProcessorTestHelper.generateTestEventWithOverrides(
                        testOverrides);

        testClusterDetailsEventProcessor.processEvent(testEvent);

        verify(mockOverridesApplier).applyOverride(overridesCaptor.capture(), anyString());

        ObjectMapper mapper = new ObjectMapper();
        ConfigOverrides capturedOverride =
                mapper.readValue(overridesCaptor.getValue(), ConfigOverrides.class);

        assertNotNull(capturedOverride.getDisable());
        assertNotNull(capturedOverride.getDisable().getDeciders());
        assertEquals(
                testOverrides.getDisable().getDeciders().size(),
                capturedOverride.getDisable().getDeciders().size());
        assertEquals(disabledDecider, capturedOverride.getDisable().getDeciders().get(0));
    }
}
