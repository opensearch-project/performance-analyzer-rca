/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer;


import io.netty.handler.codec.http.HttpMethod;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ESLocalhostConnection {

    private static final Logger LOG = LogManager.getLogger(ESLocalhostConnection.class);

    private static final int TIMEOUT_MILLIS = 30000;

    public static int makeHttpRequest(String path, HttpMethod httpMethod, String requestBody) {
        HttpURLConnection connection = null;
        try {
            connection = createHTTPConnection(path, httpMethod);
            DataOutputStream stream = new DataOutputStream(connection.getOutputStream());
            stream.writeBytes(requestBody);
            stream.flush();
            stream.close();
            return connection.getResponseCode();
        } catch (Exception e) {
            throw new RuntimeException("Request failed: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static HttpURLConnection createHTTPConnection(String path, HttpMethod httpMethod) {
        try {
            String endPoint = "https://localhost:9200" + path;
            URL endpointUrl = new URL(endPoint);
            HttpURLConnection connection = (HttpURLConnection) endpointUrl.openConnection();
            connection.setRequestMethod(httpMethod.toString());

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
