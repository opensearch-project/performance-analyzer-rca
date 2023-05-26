/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.samplers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.commons.stats.collectors.SampleAggregator;
import org.opensearch.performanceanalyzer.rca.RcaController;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaRuntimeMetrics;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessorTestHelper;

public class RcaEnabledSamplerTest {
    private RcaEnabledSampler uut;
    private AppContext appContext;

    @Mock private SampleAggregator sampleAggregator;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        appContext = new AppContext();
        uut = new RcaEnabledSampler(appContext);
    }

    @Test
    public void testIsRcaEnabled() {
        RcaController rcaController = new RcaController();
        PerformanceAnalyzerApp.setRcaController(rcaController);

        assertFalse(uut.isRcaEnabled());
        ClusterDetailsEventProcessor.NodeDetails details =
                ClusterDetailsEventProcessorTestHelper.newNodeDetails("nodex", "127.0.0.1", false);

        ClusterDetailsEventProcessor clusterDetailsEventProcessor =
                new ClusterDetailsEventProcessor();
        clusterDetailsEventProcessor.setNodesDetails(Collections.singletonList(details));
        appContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);

        assertFalse(uut.isRcaEnabled());
        details = ClusterDetailsEventProcessorTestHelper.newNodeDetails("nodey", "127.0.0.2", true);
        clusterDetailsEventProcessor.setNodesDetails(Collections.singletonList(details));
        assertEquals(rcaController.isRcaEnabled(), uut.isRcaEnabled());
    }

    @Test
    public void testSample() {
        RcaController rcaController = new RcaController();
        PerformanceAnalyzerApp.setRcaController(rcaController);

        uut.sample(sampleAggregator);
        verify(sampleAggregator, times(1))
                .updateStat(
                        RcaRuntimeMetrics.RCA_ENABLED,
                        PerformanceAnalyzerApp.getRcaController().isRcaEnabled() ? 1 : 0);
    }
}
