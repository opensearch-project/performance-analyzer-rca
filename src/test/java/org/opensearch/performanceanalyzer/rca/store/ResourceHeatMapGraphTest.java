/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensearch.performanceanalyzer.AppContext;
import org.opensearch.performanceanalyzer.ClientServers;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.commons.util.Util;
import org.opensearch.performanceanalyzer.net.GRPCConnectionManager;
import org.opensearch.performanceanalyzer.rca.RcaTestHelper;
import org.opensearch.performanceanalyzer.rca.framework.api.AnalysisGraph;
import org.opensearch.performanceanalyzer.rca.framework.api.Metric;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.CPU_Utilization;
import org.opensearch.performanceanalyzer.rca.framework.api.metrics.Heap_AllocRate;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.temperature.ClusterDimensionalSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.temperature.ClusterTemperatureSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.temperature.CompactClusterLevelNodeSummary;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.temperature.NodeLevelDimensionalSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.ConnectedComponent;
import org.opensearch.performanceanalyzer.rca.framework.core.Queryable;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.core.Stats;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.HeatZoneAssigner;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureDimension;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaUtil;
import org.opensearch.performanceanalyzer.rca.framework.util.SQLiteQueryUtils;
import org.opensearch.performanceanalyzer.rca.net.NodeStateManager;
import org.opensearch.performanceanalyzer.rca.net.ReceivedFlowUnitStore;
import org.opensearch.performanceanalyzer.rca.net.SubscriptionManager;
import org.opensearch.performanceanalyzer.rca.net.WireHopper;
import org.opensearch.performanceanalyzer.rca.persistence.Persistable;
import org.opensearch.performanceanalyzer.rca.persistence.PersistenceFactory;
import org.opensearch.performanceanalyzer.rca.scheduler.RCASchedulerTask;
import org.opensearch.performanceanalyzer.rca.store.rca.hotshard.HotShardClusterRca;
import org.opensearch.performanceanalyzer.rca.store.rca.hotshard.HotShardRca;
import org.opensearch.performanceanalyzer.rca.store.rca.temperature.ClusterTemperatureRca;
import org.opensearch.performanceanalyzer.rest.QueryRcaRequestHandler;
import org.opensearch.performanceanalyzer.util.SQLiteReader;

public class ResourceHeatMapGraphTest {
    private final int THREADS = 3;
    private static final String cwd = System.getProperty("user.dir");
    private static final Path sqliteFile =
            Paths.get(cwd, "src", "test", "resources", "metricsdbs", "metricsdb_1590716125000");

    private static final RcaConf rcaConf =
            new RcaConf(Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca.conf").toString());

    private static Queryable reader;
    private static Persistable persistable;
    private static GRPCConnectionManager connectionManager;
    private static ClientServers clientServers;

    private static SubscriptionManager subscriptionManager;
    private static AtomicReference<ExecutorService> networkThreadPoolReference;

    @BeforeClass
    public static void init() throws Exception {
        persistable = PersistenceFactory.create(rcaConf);
        reader = new SQLiteReader(sqliteFile.toString());

        AllMetrics.NodeRole nodeRole2 = AllMetrics.NodeRole.ELECTED_CLUSTER_MANAGER;
        AppContext appContext = RcaTestHelper.setMyIp("192.168.0.2", nodeRole2);
        connectionManager = new GRPCConnectionManager(false);
        clientServers =
                PerformanceAnalyzerApp.createClientServers(connectionManager, new AppContext());

        HttpServer httpServer = clientServers.getHttpServer();
        httpServer.start();

        QueryRcaRequestHandler rcaRequestHandler = new QueryRcaRequestHandler(appContext);
        rcaRequestHandler.setPersistable(persistable);
        httpServer.createContext(Util.RCA_QUERY_URL, rcaRequestHandler);

        subscriptionManager = new SubscriptionManager(connectionManager);
        networkThreadPoolReference = new AtomicReference<>();
    }

    @AfterClass
    public static void shutdown() {
        connectionManager.shutdown();
        clientServers.getHttpServer().stop(0);
        clientServers.getNetServer().stop();
        clientServers.getNetClient().stop();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    private static class AnalysisGraphX extends OpenSearchAnalysisGraph {
        @Override
        public void construct() {
            super.constructResourceHeatMapGraph();
        }
    }

    private List<ConnectedComponent> createAndExecuteRcaGraph(AppContext appContext) {
        AnalysisGraph analysisGraph = new AnalysisGraphX();
        List<ConnectedComponent> connectedComponents =
                RcaUtil.getAnalysisGraphComponents(analysisGraph);
        RcaTestHelper.setEvaluationTimeForAllNodes(connectedComponents, 1);

        String dataNodeRcaConf = Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca.conf").toString();

        RcaConf rcaConf = new RcaConf(dataNodeRcaConf);
        subscriptionManager = new SubscriptionManager(new GRPCConnectionManager(false));
        subscriptionManager.setCurrentLocus(rcaConf.getTagMap().get("locus"));

        WireHopper wireHopper =
                new WireHopper(
                        new NodeStateManager(new AppContext()),
                        clientServers.getNetClient(),
                        subscriptionManager,
                        networkThreadPoolReference,
                        new ReceivedFlowUnitStore(rcaConf.getPerVertexBufferLength()),
                        appContext);

        InstanceDetails instanceDetails = appContext.getMyInstanceDetails();
        RCASchedulerTask rcaSchedulerTaskData =
                new RCASchedulerTask(
                        1000,
                        Executors.newFixedThreadPool(THREADS),
                        connectedComponents,
                        reader,
                        persistable,
                        rcaConf,
                        wireHopper,
                        appContext);

        RcaTestHelper.setMyIp(
                instanceDetails.getInstanceIp().toString(), instanceDetails.getRole());
        rcaSchedulerTaskData.run();
        return connectedComponents;
    }

    private String makeRestRequest(final String[] params) throws Exception {
        // The params are key/value pairs and therefore there should be even numbers of them.
        Assert.assertEquals(0, params.length % 2);
        StringBuilder queryString = new StringBuilder();

        String appender = "";
        for (int i = 0; i < params.length; i += 2) {
            queryString.append(appender).append(params[i]).append("=").append(params[i + 1]);
            appender = "&";
        }
        StringBuilder uri = new StringBuilder("http://localhost:9600" + Util.RCA_QUERY_URL);
        uri.append("?").append(queryString);

        URL url = new URL(uri.toString());

        String response = "";
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        int status = connection.getResponseCode();
        if (status != 200) {
            List<String> ret =
                    new BufferedReader(new InputStreamReader(connection.getErrorStream()))
                            .lines()
                            .collect(Collectors.toList());
            throw new IllegalStateException(ret.toString());
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        response = content.toString();
        in.close();
        return response;
    }

    // @Test
    //  public void clusterTemperatureProfile() throws Exception {
    //    AppContext appContext = RcaTestHelper.setMyIp("192.168.0.2",
    // AllMetrics.NodeRole.ELECTED_CLUSTER_MANAGER);
    //
    //    List<ConnectedComponent> connectedComponents = createAndExecuteRcaGraph(appContext);
    //    System.out.println("Now for the ClusterManager RCA.");
    //    String clusterManagerNodeRcaConf =
    //        Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca_elected_cluster_manager.conf").toString();
    //    RcaConf rcaConf2 = new RcaConf(clusterManagerNodeRcaConf);
    //    SubscriptionManager subscriptionManager2 =
    //        new SubscriptionManager(new GRPCConnectionManager(false));
    //    subscriptionManager2.setCurrentLocus(rcaConf2.getTagMap().get("locus"));
    //
    //
    //    WireHopper wireHopper2 = new WireHopper(new NodeStateManager(new AppContext()),
    // clientServers.getNetClient(),
    //        subscriptionManager2,
    //        networkThreadPoolReference,
    //        new ReceivedFlowUnitStore(rcaConf.getPerVertexBufferLength()), appContext);
    //
    //    InstanceDetails instanceDetails = appContext.getMyInstanceDetails();
    //    RCASchedulerTask rcaSchedulerTaskClusterManager =
    //        new RCASchedulerTask(
    //            1000,
    //            Executors.newFixedThreadPool(THREADS),
    //            connectedComponents,
    //            reader,
    //            persistable,
    //            rcaConf2,
    //            wireHopper2,
    //            appContext);
    //    AllMetrics.NodeRole nodeRole2 = instanceDetails.getRole();
    //    RcaTestHelper.setMyIp(instanceDetails.getInstanceIp().toString(), nodeRole2);
    //    rcaSchedulerTaskClusterManager.run();
    //
    //    String resp = makeRestRequest(new String[]{"name", ClusterTemperatureRca.TABLE_NAME});
    //    testJsonResponse(resp);
    //  }

    /**
     * { "ClusterTemperatureRca":[ { "ClusterTemperatureSummary":[ { "ClusterDimensionalSummary":[ {
     * "ZoneSummary":[ { "all_nodes":[
     *
     * <p>], "max":null, "min":null, "zone":"HOT" }, { "all_nodes":[
     *
     * <p>], "max":null, "min":null, "zone":"WARM" }, { "all_nodes":[ {
     * "host_address":"192.168.0.1", "node_id":"4sqG_APMQuaQwEW17_6zwg" } ], "max":null, "min":null,
     * "zone":"LUKE_WARM" }, { "all_nodes":[
     *
     * <p>], "max":null, "min":null, "zone":"COLD" } ], "dimension":"CPU_Utilization", "mean":10,
     * "numNodes":1, "total":0.113345915412554 }, { "ZoneSummary":[ { "all_nodes":[
     *
     * <p>], "max":null, "min":null, "zone":"HOT" }, { "all_nodes":[
     *
     * <p>], "max":null, "min":null, "zone":"WARM" }, { "all_nodes":[ {
     * "host_address":"192.168.0.1", "node_id":"4sqG_APMQuaQwEW17_6zwg" } ], "max":null, "min":null,
     * "zone":"LUKE_WARM" }, { "all_nodes":[
     *
     * <p>], "max":null, "min":null, "zone":"COLD" } ], "dimension":"Heap_AllocRate", "mean":9,
     * "numNodes":1, "total":7429635.38060667 }, { "ZoneSummary":[ { "all_nodes":[
     *
     * <p>], "max":null, "min":null, "zone":"HOT" }, { "all_nodes":[
     *
     * <p>], "max":null, "min":null, "zone":"WARM" }, { "all_nodes":[ {
     * "host_address":"192.168.0.1", "node_id":"4sqG_APMQuaQwEW17_6zwg" } ], "max":null, "min":null,
     * "zone":"LUKE_WARM" }, { "all_nodes":[
     *
     * <p>], "max":null, "min":null, "zone":"COLD" } ], "dimension":"IO_READ_SYSCALL_RATE",
     * "mean":0, "numNodes":1, "total":0.0 }, { "ZoneSummary":[ { "all_nodes":[
     *
     * <p>], "max":null, "min":null, "zone":"HOT" }, { "all_nodes":[
     *
     * <p>], "max":null, "min":null, "zone":"WARM" }, { "all_nodes":[ {
     * "host_address":"192.168.0.1", "node_id":"4sqG_APMQuaQwEW17_6zwg" } ], "max":null, "min":null,
     * "zone":"LUKE_WARM" }, { "all_nodes":[
     *
     * <p>], "max":null, "min":null, "zone":"COLD" } ], "dimension":"IO_WriteSyscallRate", "mean":0,
     * "numNodes":1, "total":0.0 }, { "ZoneSummary":[ { "all_nodes":[
     *
     * <p>], "max":null, "min":null, "zone":"HOT" }, { "all_nodes":[
     *
     * <p>], "max":null, "min":null, "zone":"WARM" }, { "all_nodes":[ {
     * "host_address":"192.168.0.1", "node_id":"4sqG_APMQuaQwEW17_6zwg" } ], "max":null, "min":null,
     * "zone":"LUKE_WARM" }, { "all_nodes":[
     *
     * <p>], "max":null, "min":null, "zone":"COLD" } ], "dimension":"Shard_Size_In_Bytes",
     * "mean":10, "numNodes":1, "total":22894813.0 } ], "CompactClusterLevelNodeSummary":[ {
     * "CPU_Utilization_mean":10, "CPU_Utilization_num_shards":3,
     * "CPU_Utilization_total":0.113345915412554, "Heap_AllocRate_mean":9,
     * "Heap_AllocRate_num_shards":3, "Heap_AllocRate_total":7429635.38060667,
     * "IO_READ_SYSCALL_RATE_mean":0, "IO_READ_SYSCALL_RATE_num_shards":0,
     * "IO_READ_SYSCALL_RATE_total":0.0, "IO_WriteSyscallRate_mean":0,
     * "IO_WriteSyscallRate_num_shards":0, "IO_WriteSyscallRate_total":0.0,
     * "Shard_Size_In_Bytes_mean":10, "Shard_Size_In_Bytes_num_shards":3,
     * "Shard_Size_In_Bytes_total":22894813.0, "host_address":"192.168.0.1",
     * "node_id":"4sqG_APMQuaQwEW17_6zwg" } ] } ], "rca_name":"ClusterTemperatureRca",
     * "state":"unknown", "timestamp":1590980168474 } ], "HighHeapUsageClusterRca":[
     *
     * <p>], "HotNodeClusterRca":[
     *
     * <p>] }
     */
    private void testJsonResponse(String jsonResponse) {
        final String clusterTempRca = ClusterTemperatureRca.TABLE_NAME;
        final String clusterTempSummaryStr = ClusterTemperatureSummary.TABLE_NAME;
        final String clusterDimSummary = ClusterDimensionalSummary.TABLE_NAME;
        final String clusterZoneSummary = ClusterDimensionalSummary.ZONE_PROFILE_TABLE_NAME;
        final String clusterLevelNodeSummary = CompactClusterLevelNodeSummary.class.getSimpleName();

        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(jsonResponse);
        JsonObject temperatureRca =
                jsonElement
                        .getAsJsonObject()
                        .get(clusterTempRca)
                        .getAsJsonArray()
                        .get(0)
                        .getAsJsonObject();

        Assert.assertEquals(clusterTempRca, temperatureRca.get("rca_name").getAsString());
        Assert.assertEquals("unknown", temperatureRca.get("state").getAsString());

        JsonObject clusterTempSummary =
                temperatureRca.get(clusterTempSummaryStr).getAsJsonArray().get(0).getAsJsonObject();

        JsonArray dimensionArray = clusterTempSummary.get(clusterDimSummary).getAsJsonArray();
        for (int i = 0; i < dimensionArray.size(); i++) {
            JsonObject dimensionObj = dimensionArray.get(i).getAsJsonObject();
            if (dimensionObj.get("dimension").getAsString().equals("CPU_Utilization")) {
                int mean = dimensionObj.get("mean").getAsInt();
                Assert.assertEquals(10, mean);

                double total = dimensionObj.get("total").getAsDouble();
                Assert.assertEquals(0.113345915412554, total, 0.01);

                int numNodes = dimensionObj.get("numNodes").getAsInt();
                Assert.assertEquals(1, numNodes);

                JsonArray zoneArr = dimensionObj.get(clusterZoneSummary).getAsJsonArray();
                for (int j = 0; j < zoneArr.size(); j++) {
                    JsonObject zoneObject = zoneArr.get(j).getAsJsonObject();
                    if (zoneObject.get("zone").getAsString().equals("LUKE_WARM")) {
                        Assert.assertTrue(zoneObject.get("min").isJsonNull());
                        Assert.assertTrue(zoneObject.get("max").isJsonNull());

                        JsonArray allNodesArr = zoneObject.get("all_nodes").getAsJsonArray();

                        for (int k = 0; k < allNodesArr.size(); k++) {
                            JsonObject nodeObj = allNodesArr.get(k).getAsJsonObject();
                            // Assert.assertEquals("192.168.0.1",
                            //    nodeObj.get("host_address").getAsString());
                            // Assert.assertEquals("4sqG_APMQuaQwEW17_6zwg",
                            //    nodeObj.get("node_id").getAsString());
                        }
                    }
                }
            }
        }

        JsonArray nodeDetailsArr = clusterTempSummary.get(clusterLevelNodeSummary).getAsJsonArray();
        for (int i = 0; i < nodeDetailsArr.size(); i++) {
            JsonObject node = nodeDetailsArr.get(i).getAsJsonObject();
            // "node_id": "4sqG_APMQuaQwEW17_6zwg",
            // "host_address": "192.168.0.1",
            // "CPU_Utilization_mean": 10,
            // "CPU_Utilization_total": 0.113345915412554,
            // "CPU_Utilization_num_shards": 3,
            // "Heap_AllocRate_mean": 0,
            // "Heap_AllocRate_total": 0,
            // "Heap_AllocRate_num_shards": 0,
            // "IO_READ_SYSCALL_RATE_mean": 0,
            // "IO_READ_SYSCALL_RATE_total": 0,
            // "IO_READ_SYSCALL_RATE_num_shards": 0,
            // "IO_WriteSyscallRate_mean": 0,
            // "IO_WriteSyscallRate_total": 0,
            // "IO_WriteSyscallRate_num_shards": 0

            Assert.assertEquals("4sqG_APMQuaQwEW17_6zwg", node.get("node_id").getAsString());
            // Assert.assertEquals("192.168.0.1", node.get("host_address").getAsString());
            Assert.assertEquals(10, node.get("CPU_Utilization_mean").getAsInt());
            Assert.assertEquals(
                    0.113345915412554, node.get("CPU_Utilization_total").getAsDouble(), 0.01);
            Assert.assertEquals(3, node.get("CPU_Utilization_num_shards").getAsInt());

            Assert.assertEquals(9, node.get("Heap_AllocRate_mean").getAsInt());
            Assert.assertEquals(7429635, node.get("Heap_AllocRate_total").getAsInt());
            Assert.assertEquals(3, node.get("Heap_AllocRate_num_shards").getAsInt());
        }
    }

    @Test
    public void fullNodeTemperatureProfile() throws Exception {
        AppContext appContext = RcaTestHelper.setMyIp("192.168.0.3", AllMetrics.NodeRole.DATA);
        createAndExecuteRcaGraph(appContext);
        verifyFullNodeTemperatureProfile(
                makeRestRequest(
                        new String[] {
                            "name", SQLiteQueryUtils.ALL_TEMPERATURE_DIMENSIONS, "local", "true"
                        }));
    }

    /**
     * { "AllTemperatureDimensions":[ { "NodeLevelDimensionalSummary":[ { "NodeLevelZoneSummary":[ {
     * "all_shards":[
     *
     * <p>], "zone":"HOT" }, { "all_shards":[ { "index_name":"pmc", "shard_id":4, "temperature":[ {
     * "dimension":"CPU_Utilization", "value":"1" }, { "dimension":"Heap_AllocRate", "value":"1" },
     * { "dimension":"Shard_Size_In_Bytes", "value":"10" } ] } ], "zone":"WARM" }, { "all_shards":[
     * { "index_name":"pmc", "shard_id":0, "temperature":[ { "dimension":"CPU_Utilization",
     * "value":"0" }, { "dimension":"Heap_AllocRate", "value":"0" }, {
     * "dimension":"Shard_Size_In_Bytes", "value":"10" } ] }, { "index_name":"pmc", "shard_id":2,
     * "temperature":[ { "dimension":"CPU_Utilization", "value":"0" }, {
     * "dimension":"Heap_AllocRate", "value":"1" }, { "dimension":"Shard_Size_In_Bytes",
     * "value":"10" } ] } ], "zone":"LUKE_WARM" }, { "all_shards":[
     *
     * <p>], "zone":"COLD" } ], "dimension":"CPU_Utilization", "mean":0, "numShards":3,
     * "timestamp":"1591757320624", "total":0.383474080686612 }, { "NodeLevelZoneSummary":[ {
     * "all_shards":[
     *
     * <p>], "zone":"HOT" }, { "all_shards":[
     *
     * <p>], "zone":"WARM" }, { "all_shards":[ { "index_name":"pmc", "shard_id":2, "temperature":[ {
     * "dimension":"CPU_Utilization", "value":"0" }, { "dimension":"Heap_AllocRate", "value":"1" },
     * { "dimension":"Shard_Size_In_Bytes", "value":"3" } ] }, { "index_name":"pmc", "shard_id":4,
     * "temperature":[ { "dimension":"CPU_Utilization", "value":"1" }, {
     * "dimension":"Heap_AllocRate", "value":"1" }, { "dimension":"Shard_Size_In_Bytes", "value":"3"
     * } ] }, { "index_name":"pmc", "shard_id":0, "temperature":[ { "dimension":"CPU_Utilization",
     * "value":"0" }, { "dimension":"Heap_AllocRate", "value":"0" }, {
     * "dimension":"Shard_Size_In_Bytes", "value":"3" } ] } ], "zone":"LUKE_WARM" }, {
     * "all_shards":[
     *
     * <p>], "zone":"COLD" } ], "dimension":"Shard_Size_In_Bytes", "mean":3, "numShards":3,
     * "timestamp":"1591757345439", "total":24206839.0 }, { "NodeLevelZoneSummary":[ {
     * "all_shards":[
     *
     * <p>], "zone":"HOT" }, { "all_shards":[
     *
     * <p>], "zone":"WARM" }, { "all_shards":[ { "index_name":"pmc", "shard_id":4, "temperature":[ {
     * "dimension":"CPU_Utilization", "value":"1" }, { "dimension":"Heap_AllocRate", "value":"1" },
     * { "dimension":"Shard_Size_In_Bytes", "value":"10" } ] }, { "index_name":"pmc", "shard_id":2,
     * "temperature":[ { "dimension":"CPU_Utilization", "value":"0" }, {
     * "dimension":"Heap_AllocRate", "value":"1" }, { "dimension":"Shard_Size_In_Bytes",
     * "value":"10" } ] }, { "index_name":"pmc", "shard_id":0, "temperature":[ {
     * "dimension":"CPU_Utilization", "value":"0" }, { "dimension":"Heap_AllocRate", "value":"0" },
     * { "dimension":"Shard_Size_In_Bytes", "value":"10" } ] } ], "zone":"LUKE_WARM" }, {
     * "all_shards":[
     *
     * <p>], "zone":"COLD" } ], "dimension":"Heap_AllocRate", "mean":1, "numShards":3,
     * "timestamp":"1591757317017", "total":3557053.99136461 } ] } ] }
     */
    private void verifyFullNodeTemperatureProfile(String resp) {
        JsonParser parser = new JsonParser();
        JsonArray json =
                parser.parse(resp)
                        .getAsJsonObject()
                        .getAsJsonArray(SQLiteQueryUtils.ALL_TEMPERATURE_DIMENSIONS);
        System.out.println("ALL_TEMPERATURE_DIMENSIONS" + json);

        for (JsonElement elem :
                json.get(0)
                        .getAsJsonObject()
                        .getAsJsonArray(NodeLevelDimensionalSummary.SUMMARY_TABLE_NAME)) {
            JsonObject object = elem.getAsJsonObject();
            System.out.println(object);
            System.out.println("dim obj: " + object.get("dimension"));
            switch (TemperatureDimension.valueOf(object.get("dimension").getAsString())) {
                case CPU_Utilization:
                    verifyCpuDimension(object);
                    break;
                case Heap_AllocRate:
                    verifyHeapAllocDimension(object);
                    break;
                case Shard_Size_In_Bytes:
                    verifyShardSizeDimension(object);
                    break;
            }
        }
    }

    private void verifyCpuDimension(JsonObject cpuObject) {
        Assert.assertEquals(1, cpuObject.get("mean").getAsInt());
        Assert.assertEquals(0.113345915412554, cpuObject.get("total").getAsDouble(), 0.01);
        Assert.assertEquals(3, cpuObject.get("numShards").getAsInt());

        for (JsonElement elem : cpuObject.getAsJsonArray("NodeLevelZoneSummary")) {
            JsonObject o = elem.getAsJsonObject();
            switch (HeatZoneAssigner.Zone.valueOf(o.get("zone").getAsString())) {
                case HOT:
                    Assert.assertEquals(0, o.getAsJsonArray("all_shards").size());
                    break;
                case WARM:
                    {
                        Assert.assertEquals(0, o.getAsJsonArray("all_shards").size());
                        break;
                    }
                case LUKE_WARM:
                    Assert.assertEquals(3, o.getAsJsonArray("all_shards").size());
                    for (JsonElement e : o.getAsJsonArray("all_shards")) {
                        Assert.assertEquals(
                                "pmc", e.getAsJsonObject().get("index_name").getAsString());
                        int shardId = e.getAsJsonObject().get("shard_id").getAsInt();
                        Assert.assertTrue(shardId == 2 || shardId == 4 || shardId == 0);
                    }
                    break;
                case COLD:
                    Assert.assertEquals(0, o.getAsJsonArray("all_shards").size());
            }
        }
    }

    private void verifyHeapAllocDimension(JsonObject cpuObject) {
        Assert.assertEquals(1, cpuObject.get("mean").getAsInt());
        Assert.assertEquals(7429635.38060667, cpuObject.get("total").getAsDouble(), 0.01);
        Assert.assertEquals(3, cpuObject.get("numShards").getAsInt());

        for (JsonElement elem : cpuObject.getAsJsonArray("NodeLevelZoneSummary")) {
            JsonObject o = elem.getAsJsonObject();
            switch (HeatZoneAssigner.Zone.valueOf(o.get("zone").getAsString())) {
                case HOT:
                    Assert.assertEquals(0, o.getAsJsonArray("all_shards").size());
                    break;
                case WARM:
                    {
                        Assert.assertEquals(0, o.getAsJsonArray("all_shards").size());
                        break;
                    }
                case LUKE_WARM:
                    Assert.assertEquals(3, o.getAsJsonArray("all_shards").size());
                    for (JsonElement e : o.getAsJsonArray("all_shards")) {
                        Assert.assertEquals(
                                "pmc", e.getAsJsonObject().get("index_name").getAsString());
                        int shardId = e.getAsJsonObject().get("shard_id").getAsInt();
                        Assert.assertTrue(shardId == 2 || shardId == 4 || shardId == 0);
                    }
                    break;
                case COLD:
                    Assert.assertEquals(0, o.getAsJsonArray("all_shards").size());
            }
        }
    }

    private void verifyShardSizeDimension(JsonObject shardSizeObj) {
        Assert.assertEquals(3, shardSizeObj.get("mean").getAsInt());
        Assert.assertEquals(2.2894812999999993E7, shardSizeObj.get("total").getAsDouble(), 0.01);
        Assert.assertEquals(3, shardSizeObj.get("numShards").getAsInt());

        for (JsonElement elem : shardSizeObj.getAsJsonArray("NodeLevelZoneSummary")) {
            JsonObject o = elem.getAsJsonObject();
            switch (HeatZoneAssigner.Zone.valueOf(o.get("zone").getAsString())) {
                case HOT:
                    Assert.assertEquals(0, o.getAsJsonArray("all_shards").size());
                    break;
                case WARM:
                    {
                        Assert.assertEquals(0, o.getAsJsonArray("all_shards").size());
                        break;
                    }
                case LUKE_WARM:
                    Assert.assertEquals(3, o.getAsJsonArray("all_shards").size());
                    for (JsonElement e : o.getAsJsonArray("all_shards")) {
                        Assert.assertEquals(
                                "pmc", e.getAsJsonObject().get("index_name").getAsString());
                        int shardId = e.getAsJsonObject().get("shard_id").getAsInt();
                        System.out.println("ShardID " + shardId);
                        Assert.assertTrue(shardId == 2 || shardId == 4 || shardId == 0);
                    }
                    break;
                case COLD:
                    Assert.assertEquals(0, o.getAsJsonArray("all_shards").size());
            }
        }
    }

    // If the Temperature profile rca is muted, we expect it to return something like:
    // {"AllTemperatureDimensions":[]}
    @Test
    public void mutedTemperatureProfile() {
        AppContext appContext =
                RcaTestHelper.setMyIp("192.168.0.7", AllMetrics.NodeRole.CLUSTER_MANAGER);
        createAndExecuteRcaGraph(appContext);

        Set<String> oldMuted = new HashSet<>();
        for (String muted : Stats.getInstance().getMutedGraphNodes()) {
            oldMuted.add(muted);
        }

        Stats.getInstance()
                .updateMutedGraphNodes(
                        new HashSet<>(
                                Collections.singletonList(
                                        SQLiteQueryUtils.ALL_TEMPERATURE_DIMENSIONS)));

        try {
            makeRestRequest(
                    new String[] {
                        "name", SQLiteQueryUtils.ALL_TEMPERATURE_DIMENSIONS, "local", "true"
                    });
        } catch (Exception ex) {
            String msg = ex.getMessage();
            JsonParser parser = new JsonParser();
            Assert.assertEquals(
                    "The Rca 'AllTemperatureDimensions' is muted. Consider removing "
                            + "it from the rca.conf's 'muted-rcas' list",
                    parser.parse(msg)
                            .getAsJsonArray()
                            .get(0)
                            .getAsJsonObject()
                            .get("error")
                            .getAsString());
        }
        // Setting it back to what it used to be.
        Stats.getInstance().updateMutedGraphNodes(oldMuted);
        System.out.println(Stats.getInstance().getMutedGraphNodes());
    }

    private static class AnalysisGraphHotShard extends OpenSearchAnalysisGraph {
        @Override
        public void construct() {
            Metric cpuUtilization = new CPU_Utilization(1);

            cpuUtilization.addTag(
                    RcaConsts.RcaTagConstants.TAG_LOCUS,
                    RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);

            addLeaf(cpuUtilization);

            // Hot Shard RCA consuming CPU_Utilization and Heap_AllocRate
            HotShardRca hotShardRca = new HotShardRca(1, 1, cpuUtilization);
            hotShardRca.addTag(
                    RcaConsts.RcaTagConstants.TAG_LOCUS,
                    RcaConsts.RcaTagConstants.LOCUS_DATA_CLUSTER_MANAGER_NODE);
            hotShardRca.addAllUpstreams(Arrays.asList(cpuUtilization));

            // Hot Shard Cluster RCA which consumes the above
            HotShardClusterRca hotShardClusterRca = new HotShardClusterRca(1, hotShardRca);
            hotShardClusterRca.addTag(
                    RcaConsts.RcaTagConstants.TAG_LOCUS,
                    RcaConsts.RcaTagConstants.LOCUS_CLUSTER_MANAGER_NODE);
            hotShardClusterRca.addAllUpstreams(Collections.singletonList(hotShardRca));
            hotShardClusterRca.addTag(
                    RcaConsts.RcaTagConstants.TAG_AGGREGATE_UPSTREAM,
                    RcaConsts.RcaTagConstants.LOCUS_DATA_NODE);
        }
    }

    @Test
    public void testHotShardClusterApiResponse() throws Exception {
        AnalysisGraph analysisGraph = new AnalysisGraphHotShard();
        List<ConnectedComponent> connectedComponents =
                RcaUtil.getAnalysisGraphComponents(analysisGraph);
        RcaTestHelper.setEvaluationTimeForAllNodes(connectedComponents, 1);

        String dataNodeRcaConf = Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca.conf").toString();

        RcaConf rcaConf = new RcaConf(dataNodeRcaConf);
        SubscriptionManager subscriptionManager =
                new SubscriptionManager(new GRPCConnectionManager(false));
        subscriptionManager.setCurrentLocus(rcaConf.getTagMap().get("locus"));

        AppContext appContext = RcaTestHelper.setMyIp("192.168.0.1", AllMetrics.NodeRole.DATA);

        WireHopper wireHopper =
                new WireHopper(
                        new NodeStateManager(new AppContext()),
                        clientServers.getNetClient(),
                        subscriptionManager,
                        networkThreadPoolReference,
                        new ReceivedFlowUnitStore(rcaConf.getPerVertexBufferLength()),
                        appContext);
        InstanceDetails dataInstance = appContext.getMyInstanceDetails();
        RCASchedulerTask rcaSchedulerTaskData =
                new RCASchedulerTask(
                        1000,
                        Executors.newFixedThreadPool(THREADS),
                        connectedComponents,
                        reader,
                        persistable,
                        rcaConf,
                        wireHopper,
                        appContext);
        AllMetrics.NodeRole nodeRole = dataInstance.getRole();
        RcaTestHelper.setMyIp(dataInstance.getInstanceIp().toString(), nodeRole);
        rcaSchedulerTaskData.run();

        String clusterManagerNodeRcaConf =
                Paths.get(RcaConsts.TEST_CONFIG_PATH, "rca_elected_cluster_manager.conf")
                        .toString();
        RcaConf rcaConf2 = new RcaConf(clusterManagerNodeRcaConf);
        SubscriptionManager subscriptionManager2 =
                new SubscriptionManager(new GRPCConnectionManager(false));
        subscriptionManager2.setCurrentLocus(rcaConf2.getTagMap().get("locus"));

        AppContext appContextClusterManager =
                RcaTestHelper.setMyIp("192.168.0.4", AllMetrics.NodeRole.ELECTED_CLUSTER_MANAGER);

        WireHopper wireHopper2 =
                new WireHopper(
                        new NodeStateManager(new AppContext()),
                        clientServers.getNetClient(),
                        subscriptionManager2,
                        networkThreadPoolReference,
                        new ReceivedFlowUnitStore(rcaConf.getPerVertexBufferLength()),
                        appContextClusterManager);

        InstanceDetails clusterManagerInstance = appContextClusterManager.getMyInstanceDetails();
        RCASchedulerTask rcaSchedulerTaskClusterManager =
                new RCASchedulerTask(
                        1000,
                        Executors.newFixedThreadPool(THREADS),
                        connectedComponents,
                        reader,
                        persistable,
                        rcaConf2,
                        wireHopper2,
                        appContextClusterManager);
        AllMetrics.NodeRole nodeRole2 = clusterManagerInstance.getRole();
        RcaTestHelper.setMyIp(clusterManagerInstance.getInstanceIp().toString(), nodeRole2);
        rcaSchedulerTaskClusterManager.run();

        URL url = null;
        url =
                new URL(
                        "http://localhost:9600"
                                + Util.RCA_QUERY_URL
                                + "?name="
                                + HotShardClusterRca.RCA_TABLE_NAME);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        int status = con.getResponseCode();
        System.out.println("Response status: " + status);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            final String hotShardClusterRcaName = HotShardClusterRca.RCA_TABLE_NAME;
            final String hotClusterSummaryName = HotClusterSummary.HOT_CLUSTER_SUMMARY_TABLE;

            JsonParser parser = new JsonParser();
            JsonElement jsonElement = parser.parse(content.toString());
            JsonObject hotShardClusterRca =
                    jsonElement
                            .getAsJsonObject()
                            .get(hotShardClusterRcaName)
                            .getAsJsonArray()
                            .get(0)
                            .getAsJsonObject();

            Assert.assertEquals(
                    hotShardClusterRcaName, hotShardClusterRca.get("rca_name").getAsString());
            Assert.assertEquals("unhealthy", hotShardClusterRca.get("state").getAsString());

            JsonObject hotClusterSummary =
                    hotShardClusterRca
                            .get(hotClusterSummaryName)
                            .getAsJsonArray()
                            .get(0)
                            .getAsJsonObject();
            Assert.assertEquals(1, hotClusterSummary.get("number_of_unhealthy_nodes").getAsInt());
        }
        con.disconnect();
    }
}
