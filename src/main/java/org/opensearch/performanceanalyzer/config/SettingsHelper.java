/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.config;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SettingsHelper {
    public static Properties getSettings(final String fileAbsolutePath) throws IOException {
        Properties prop = new Properties();

        try (InputStream input = new FileInputStream(fileAbsolutePath); ) {
            // load a properties file
            prop.load(input);
        }

        return prop;
    }
}
