/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.util;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.commons.metrics.PerformanceAnalyzerMetrics;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ReaderMetrics;

public class FileHelper {
    private static final Logger log = LogManager.getLogger(FileHelper.class);
    private static boolean jvmSupportMillisecondFileModityTime = true;
    private static long SECOND_TO_MILLISECONDS = 1000;

    static {
        try {
            // Create tmp file and test if we can read millisecond
            for (int i = 0; i < 2; i++) {
                File tmpFile = File.createTempFile("performanceanalyzer", ".tmp");
                tmpFile.deleteOnExit();
                jvmSupportMillisecondFileModityTime = tmpFile.lastModified() % 1000 != 0;
                if (jvmSupportMillisecondFileModityTime) {
                    break;
                }
                Thread.sleep(2);
            }
        } catch (Exception ex) {
            log.error("Having issue creating tmp file. Using default value.", ex);
        }
        log.info("jvmSupportMillisecondFileModityTime: {}", jvmSupportMillisecondFileModityTime);
    }

    public static long getLastModified(File file, long startTime, long endTime) {
        if (!file.isFile() || jvmSupportMillisecondFileModityTime) {
            return file.lastModified();
        }

        if (file.lastModified() < startTime - SECOND_TO_MILLISECONDS
                || file.lastModified() > endTime) {
            return file.lastModified();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null) {
                String[] fields = line.split(PerformanceAnalyzerMetrics.sKeyValueDelimitor);
                if (fields[0].equals(PerformanceAnalyzerMetrics.METRIC_CURRENT_TIME)) {
                    return Long.parseLong(fields[1]);
                }
            }
        } catch (Exception ex) {
            PerformanceAnalyzerApp.READER_METRICS_AGGREGATOR.updateStat(ReaderMetrics.OTHER, "", 1);
            log.debug(
                    "Having issue to read current time from the content of file. Using file metadata; exception: {} ExceptionCode: {}",
                    () -> ex,
                    () -> ReaderMetrics.OTHER.toString());
        }
        return file.lastModified();
    }
}
