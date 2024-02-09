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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.RcaController;
import org.opensearch.performanceanalyzer.rca.framework.core.ConnectedComponent;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.core.Stats;
import org.opensearch.performanceanalyzer.rca.store.rca.hotshard.HotShardClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.temperature.NodeTemperatureRca;
import org.opensearch.performanceanalyzer.reader.ClusterDetailsEventProcessor;

public class QueryRcaRequestHandlerTest {
    private QueryRcaRequestHandler handler;
    private AppContext appContext;
    private RcaController rcaController;
    private static final String queryPrefix =
            "http://localhost:9600/_plugins/_performanceanalyzer/rca";
    private static final String mutedConfPath =
            "./src/test/resources/rca/rca_query_muted_test.conf";
    private static final String nonMutedConfPath = "./src/test/resources/rca/rca_query_test.conf";

    private void setClusterManagerContext(boolean isClusterManager) {
        ClusterDetailsEventProcessor clusterDetailsEventProcessor =
                new ClusterDetailsEventProcessor();
        clusterDetailsEventProcessor.setNodesDetails(
                Collections.singletonList(
                        new ClusterDetailsEventProcessor.NodeDetails(
                                isClusterManager
                                        ? AllMetrics.NodeRole.ELECTED_CLUSTER_MANAGER
                                        : AllMetrics.NodeRole.DATA,
                                "test_node",
                                "127.0.0.1",
                                isClusterManager)));
        appContext.setClusterDetailsEventProcessor(clusterDetailsEventProcessor);
    }

    private void setConfPath(String rcaConfPath) {
        rcaController.setRcaConf(new RcaConf(rcaConfPath));
        rcaController.updateMutedComponents();
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
        /* Prepares RcaController for with nodes and config files for testing */
        appContext = new AppContext();
        rcaController = new RcaController();
        ConnectedComponent connectedComponent = new ConnectedComponent(1);
        connectedComponent.addLeafNode(new HotShardClusterRca(0, null));
        connectedComponent.addLeafNode(new NodeTemperatureRca(null, null, null));
        rcaController.setConnectedComponents(Collections.singletonList(connectedComponent));
        connectedComponent.getAllNodes(); // Initializes node names
        Stats.getInstance().getConnectedComponents(); // Initializes muted graph nodes structure
        setConfPath(nonMutedConfPath);
        PerformanceAnalyzerApp.setRcaController(rcaController);
        handler = new QueryRcaRequestHandler(appContext);
        setClusterManagerContext(false);
    }

    @After
    public void reset() {
        Stats.getInstance().getConnectedComponents();
        PerformanceAnalyzerApp.setRcaController(null);
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
    public void testMutedLocalTemperatureRCA() throws Exception {
        setConfPath(mutedConfPath);
        Assert.assertTrue(Stats.getInstance().getMutedGraphNodes().contains("NodeTemperatureRca"));
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

    @Test
    public void testInvalidParams() throws Exception {
        setClusterManagerContext(true);
        OutputStream exchangeOutputStream = new ByteArrayOutputStream();
        HttpExchange exchange =
                sendQuery(queryPrefix + "?name=NonExistingClusterRCA", "GET", exchangeOutputStream);
        Mockito.verify(exchange)
                .sendResponseHeaders(
                        ArgumentMatchers.eq(HttpURLConnection.HTTP_BAD_REQUEST),
                        ArgumentMatchers.anyLong());

        Assert.assertTrue(exchangeOutputStream.toString().contains("Invalid RCA"));
    }

    @Test
    public void testMutedClusterRCA() throws Exception {
        setClusterManagerContext(true);
        setConfPath(mutedConfPath);
        Assert.assertTrue(Stats.getInstance().getMutedGraphNodes().contains("NodeTemperatureRca"));
        OutputStream exchangeOutputStream = new ByteArrayOutputStream();
        HttpExchange exchange =
                sendQuery(queryPrefix + "?name=HotShardClusterRca", "GET", exchangeOutputStream);
        Mockito.verify(exchange)
                .sendResponseHeaders(
                        ArgumentMatchers.eq(HttpURLConnection.HTTP_OK), ArgumentMatchers.anyLong());

        Assert.assertEquals("{}", exchangeOutputStream.toString());
    }
}
