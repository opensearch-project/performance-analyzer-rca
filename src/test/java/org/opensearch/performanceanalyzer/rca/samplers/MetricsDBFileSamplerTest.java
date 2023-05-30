/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.samplers;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.commons.stats.SampleAggregator;
import org.opensearch.performanceanalyzer.metricsdb.MetricsDB;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ReaderMetrics;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MetricsDB.class, MetricsDBFileSampler.class})
@PowerMockIgnore({
    "com.sun.org.apache.xerces.*",
    "javax.xml.*",
    "org.xml.*",
    "javax.management.*",
    "org.w3c.*"
})
public class MetricsDBFileSamplerTest {
    private MetricsDBFileSamplerTest uut;
    private AppContext appContext;

    @Mock private SampleAggregator sampleAggregator;

    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testSample() throws IOException {
        // Setup metrics folder
        File metricsFolder = folder.newFolder("metrics");
        String metricsFolderPath = metricsFolder.getPath();

        // Setup matched and ignored metricsdb files
        String metricsdbFilePrefix = Paths.get(metricsFolderPath, "metricsdb_").toString();
        String[] randomFilenames = {
            "performance_analyzer_agent_stats.log",
            "PerformanceAnalyzer.log",
            "opensearch-10045690226637554248",
            "ks-script-56tHfe"
        };
        String[] closeMatchFilenames = {
            "metricsdb_",
            "metricsdb_abc",
            "metricsdb_1607650185000a",
            "metricsdb1_1607650185000",
            "metricsdb.tar.2020-12-10-17.gz",
            "metricsdb_.tar.2020-12-10-17-45.gz",
            "metricsdb.tar1.2020-12-10-17-45.gz",
            "metricsdb.tar.2020-12-10-17-45.gz1"
        };
        String[] metricsdbFiles = {
            "metricsdb_1607650180000", "metricsdb_1607650185000", "metricsdb_1607650190000"
        };
        String[] metricsdbTarFiles = {
            "metricsdb.tar.2020-12-10-17-35.gz", "metricsdb.tar.2020-12-10-17-40.gz"
        };

        long metricsdbFileSize = 8 * 1024;
        long metricsdbTarFileSize = 16 * 1024;

        for (String fname : randomFilenames) {
            Files.createFile(Paths.get(metricsFolderPath, fname));
        }
        for (String fname : closeMatchFilenames) {
            Files.createFile(Paths.get(metricsFolderPath, fname));
        }
        for (String fname : metricsdbFiles) {
            RandomAccessFile f =
                    new RandomAccessFile(Paths.get(metricsFolderPath, fname).toString(), "rw");
            f.setLength(metricsdbFileSize);
            f.close();
        }
        for (String fname : metricsdbTarFiles) {
            RandomAccessFile f =
                    new RandomAccessFile(Paths.get(metricsFolderPath, fname).toString(), "rw");
            f.setLength(metricsdbTarFileSize);
            f.close();
        }

        // Create spy for MetricsDB utils used by sampler
        spy(MetricsDB.class);
        when(MetricsDB.getFilePrefix()).thenReturn(metricsdbFilePrefix);

        // Test sampler
        AppContext appContext = new AppContext();
        MetricsDBFileSampler uut = new MetricsDBFileSampler(appContext);
        spy(MetricsDB.class);
        when(MetricsDB.getFilePrefix()).thenReturn(metricsFolder.getPath());
        uut.sample(sampleAggregator);
        verify(sampleAggregator, times(1))
                .updateStat(
                        ReaderMetrics.METRICSDB_NUM_FILES,
                        "",
                        metricsdbFiles.length + metricsdbTarFiles.length);
        verify(sampleAggregator, times(1))
                .updateStat(
                        ReaderMetrics.METRICSDB_SIZE_FILES,
                        "",
                        metricsdbFiles.length * metricsdbFileSize
                                + metricsdbTarFiles.length * metricsdbTarFileSize);
        verify(sampleAggregator, times(1))
                .updateStat(
                        ReaderMetrics.METRICSDB_NUM_UNCOMPRESSED_FILES, "", metricsdbFiles.length);
        verify(sampleAggregator, times(1))
                .updateStat(
                        ReaderMetrics.METRICSDB_SIZE_UNCOMPRESSED_FILES,
                        "",
                        metricsdbFiles.length * metricsdbFileSize);
    }
}
