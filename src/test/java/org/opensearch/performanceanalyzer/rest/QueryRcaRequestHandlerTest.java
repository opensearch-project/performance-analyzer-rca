/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rest;


import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.core.Stats;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;

public class QueryRcaRequestHandlerTest {
    private QueryRcaRequestHandler handler;
    private AppContext appContext;
    private final String queryPrefix = "http://localhost:9600/_plugins/_performanceanalyzer/rca";

    private void setClusterManagerContext(boolean isClusterManager) {
        ClusterDetailsEventProcessor clusterDetailsEventProcessor =
                new ClusterDetailsEventProcessor();
        clusterDetailsEventProcessor.setNodesDetails(
                Collections.singletonList(
                        new ClusterDetailsEventProcessor.NodeDetails(
                                isClusterManager
                                        ? AllMetrics.NodeRole.ELECTED_CLUSTER_MANAGER
                                        : AllMetrics.NodeRole.UNKNOWN,
                                "test_node",
                                "127.0.0.1",
                                isClusterManager)));
        appContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);
    }

    private HttpExchange sendQuery(String query, String requestMethod, OutputStream os)
            throws Exception {
        HttpExchange exchange = Mockito.mock(HttpExchange.class);
        Mockito.when(exchange.getResponseBody()).thenReturn(os != null ? os : System.out);
        Mockito.when(exchange.getRequestMethod()).thenReturn(requestMethod);
        Headers responseHeaders = Mockito.mock(Headers.class);
        Mockito.when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        Mockito.when(exchange.getRequestURI()).thenReturn(new URI(query));
        handler.handle(exchange);
        return exchange;
    }

    @Before
    public void setUp() {
        appContext = new AppContext();
        handler = new QueryRcaRequestHandler(appContext);
        setClusterManagerContext(false);
    }

    @Test
    public void testInvalidNodeRole() throws Exception {
        HttpExchange exchange = sendQuery(queryPrefix + "?name=HotShardClusterRca", "GET", null);
        Mockito.verify(exchange)
                .sendResponseHeaders(
                        ArgumentMatchers.eq(HttpURLConnection.HTTP_BAD_REQUEST),
                        ArgumentMatchers.anyLong());
    }

    @Test
    public void testValidNodeRole() throws Exception {
        setClusterManagerContext(true);
        HttpExchange exchange = sendQuery(queryPrefix + "?name=HotShardClusterRca", "GET", null);
        Mockito.verify(exchange)
                .sendResponseHeaders(
                        ArgumentMatchers.eq(HttpURLConnection.HTTP_OK), ArgumentMatchers.anyLong());
    }

    @Test
    public void testBadRequestMethod() throws Exception {
        HttpExchange exchange = sendQuery(queryPrefix + "?name=HotShardClusterRca", "PUT", null);
        Mockito.verify(exchange)
                .sendResponseHeaders(
                        ArgumentMatchers.eq(HttpURLConnection.HTTP_NOT_FOUND),
                        ArgumentMatchers.anyLong());
    }

    @Test
    public void mutedLocalTemperatureRCA() throws Exception {
        Stats.getInstance().getConnectedComponents(); // Initializes muted graph nodes
        Stats.getInstance().addToMutedGraphNodes("NodeTemperatureRca");
        OutputStream exchangeOutputStream = new ByteArrayOutputStream();
        HttpExchange exchange =
                sendQuery(
                        queryPrefix + "?name=NodeTemperatureRca&local=true",
                        "GET",
                        exchangeOutputStream);
        Mockito.verify(exchange)
                .sendResponseHeaders(
                        ArgumentMatchers.eq(HttpURLConnection.HTTP_BAD_REQUEST),
                        ArgumentMatchers.anyLong());

        Assert.assertTrue(exchangeOutputStream.toString().contains("muted"));
    }
}
