/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class FileHandlerTests {
    public String rootLocation;

    @Before
    public void setup() {
        rootLocation = "build/resources/test/reader/";
    }

    @Test
    public void testFindFiles() throws IOException {
        FileHandler fileHandler = MetricPropertiesConfig.createFileHandler("indices", "path_elem");
        fileHandler.setRootLocation(rootLocation);

        List<File> files = fileHandler.findFiles4Metric(15000000000000L);
        assertEquals(1, files.size());
    }

    @Test
    public void testExtraDimensions() throws IOException {
        FileHandler shardStateFileHandler = new MetricPropertiesConfig.ShardStatFileHandler();
        shardStateFileHandler.setRootLocation(rootLocation);

        File file = new File(rootLocation + "15000000000000/indices/path_elem/123");

        String[] extraDimensions = shardStateFileHandler.processExtraDimensions(file);
        assertEquals(2, extraDimensions.length);
    }
}
