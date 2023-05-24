/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.samplers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.commons.stats.SampleAggregator;
import org.opensearch.performanceanalyzer.commons.util.Util;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ReaderMetrics;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessorTestHelper;
import org.opensearch.performanceanalyzer.reader.ReaderMetricsProcessor;

public class BatchMetricsEnabledSamplerTest {
    private static Path batchMetricsEnabledConfFile;
    private static String rootLocation;
    private static ReaderMetricsProcessor mp;
    private static AppContext appContext;
    private static BatchMetricsEnabledSampler uut;

    @Mock private SampleAggregator sampleAggregator;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Files.createDirectories(Paths.get(Util.DATA_DIR));
        batchMetricsEnabledConfFile =
                Paths.get(Util.DATA_DIR, ReaderMetricsProcessor.BATCH_METRICS_ENABLED_CONF_FILE);
        Files.deleteIfExists(batchMetricsEnabledConfFile);

        rootLocation = "build/resources/test/reader/";
        mp = new ReaderMetricsProcessor(rootLocation);
        ReaderMetricsProcessor.setCurrentInstance(mp);

        appContext = new AppContext();
        uut = new BatchMetricsEnabledSampler(appContext);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void writeBatchMetricsEnabled(boolean enabled) throws IOException {
        Files.write(batchMetricsEnabledConfFile, Boolean.toString(enabled).getBytes());
    }

    private void clearBatchMetricsEnabled() throws IOException {
        Files.deleteIfExists(batchMetricsEnabledConfFile);
    }

    @Test
    public void testIsBatchMetricsEnabled_notClusterManager() {
        appContext.setClusterDetailsEventProcessor(null);
        assertFalse(uut.isBatchMetricsEnabled());
    }

    @Test
    public void testIsBatchMetricsEnabled() throws IOException {
        ClusterDetailsEventProcessor clusterDetailsEventProcessor =
                new ClusterDetailsEventProcessor();
        ClusterDetailsEventProcessor.NodeDetails details =
                ClusterDetailsEventProcessorTestHelper.newNodeDetails("nodex", "127.0.0.1", true);
        clusterDetailsEventProcessor.setNodesDetails(Collections.singletonList(details));
        appContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);

        // No batch metrics enabled file
        clearBatchMetricsEnabled();
        mp.readBatchMetricsEnabledFromConfShim();
        assertFalse(uut.isBatchMetricsEnabled());

        // Batch metrics disabled
        writeBatchMetricsEnabled(false);
        mp.readBatchMetricsEnabledFromConfShim();
        assertFalse(uut.isBatchMetricsEnabled());

        // Batch metrics disabled
        writeBatchMetricsEnabled(true);
        mp.readBatchMetricsEnabledFromConfShim();
        assertTrue(uut.isBatchMetricsEnabled());
    }

    @Test
    public void testSample() {
        ClusterDetailsEventProcessor clusterDetailsEventProcessor =
                new ClusterDetailsEventProcessor();
        ClusterDetailsEventProcessor.NodeDetails details =
                ClusterDetailsEventProcessorTestHelper.newNodeDetails("nodex", "127.0.0.1", true);
        clusterDetailsEventProcessor.setNodesDetails(Collections.singletonList(details));
        appContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);

        uut.sample(sampleAggregator);
        verify(sampleAggregator, times(1))
                .updateStat(
                        ReaderMetrics.BATCH_METRICS_ENABLED,
                        "",
                        mp.getBatchMetricsEnabled() ? 1 : 0);
    }
}
