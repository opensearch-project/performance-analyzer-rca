/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer;

import io.netty.handler.codec.http.HttpMethod;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.core.Util;

public class LocalhostConnectionUtil {

    private static final int TIMEOUT_MILLIS = 30000;

    private static final Logger LOG = LogManager.getLogger(LocalhostConnectionUtil.class);

    public static void disablePA() throws InterruptedException {
        String PA_CONFIG_PATH = Util.PA_BASE_URL + "/cluster/config";
        String PA_DISABLE_PAYLOAD = "{\"enabled\": false}";
        int retryCount = 5;

        while (retryCount > 0) {
            HttpURLConnection connection = null;
            try {
                connection = createHTTPConnection(PA_CONFIG_PATH, HttpMethod.POST);
                DataOutputStream stream = new DataOutputStream(connection.getOutputStream());
                stream.writeBytes(PA_DISABLE_PAYLOAD);
                stream.flush();
                stream.close();
                LOG.info(
                        "PA Disable Response: "
                                + connection.getResponseCode()
                                + " "
                                + connection.getResponseMessage());
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    return;
                }
            } catch (Exception e) {
                LOG.error("PA Disable Request failed: " + e.getMessage(), e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            --retryCount;
            Thread.sleep((int) (60000 * (Math.random() * 2) + 100));
        }
        throw new RuntimeException("Failed to disable PA after 5 attempts");
    }

    public static class ClusterSettings {
        static List<String> clusterSettings = new ArrayList<>();

        public static final String SETTING_NOT_FOUND = "NULL";

        static final String CLUSTER_SETTINGS_URL =
                "/_cluster/settings?flat_settings=true&include_defaults=true&pretty";

        /** Refreshes the Cluster Settings */
        private static void refreshClusterSettings() {
            final HttpURLConnection urlConnection =
                    LocalhostConnectionUtil.createHTTPConnection(
                            CLUSTER_SETTINGS_URL, HttpMethod.GET);
            try (final BufferedReader br =
                    new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
                String line;
                clusterSettings.clear();
                while ((line = br.readLine()) != null) {
                    clusterSettings.add(line);
                }
            } catch (IOException e) {
                LOG.warn("could not refresh the cluster settings");
            }
        }

        /**
         * @param settingName a string representing the setting name in cluster settings, expected
         *     values are flat settings, e,g; <code>search_backpressure.node_duress.heap_threshold
         *     </code>
         * @param settingValRegex a regex value representing valid regex match for the setting val
         *     and should encapsulate the value in a group inside the string settingValRegex, e,g;
         *     "\"([0-9].[0-9]+)\"" to match any floating value with one leading digit
         * @returns the value for the setting settingName if present e,g; "0.7" or else NULL
         */
        public static String getClusterSettingValue(String settingName, String settingValRegex) {
            refreshClusterSettings();
            Pattern settingValPattern = Pattern.compile(settingValRegex);
            Optional<String> setting =
                    clusterSettings.stream()
                            .filter(settingLine -> settingLine.contains(settingName))
                            .findFirst();
            final String settingVal =
                    setting.map(
                                    settingLine -> {
                                        Matcher settingValMatcher =
                                                settingValPattern.matcher(settingLine);
                                        if (settingValMatcher.find()) {
                                            return settingValMatcher.group(1);
                                        }
                                        return null;
                                    })
                            .orElseGet(() -> SETTING_NOT_FOUND);
            return settingVal;
        }
    }

    private static HttpURLConnection createHTTPConnection(String path, HttpMethod httpMethod) {
        try {
            String endPoint = "http://localhost:9200" + path;
            URL endpointUrl = new URL(endPoint);
            HttpURLConnection connection = (HttpURLConnection) endpointUrl.openConnection();
            connection.setRequestMethod(httpMethod.toString());
            connection.setRequestProperty("Content-Type", "application/json");

            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            return connection;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create OpenSearch Connection: " + e.getMessage(), e);
        }
    }
}
