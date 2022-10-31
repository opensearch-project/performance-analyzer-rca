/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.framework;


import com.google.gson.JsonElement;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.apache.commons.io.FileUtils;
import org.jooq.Record;
import org.jooq.Result;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.config.PluginSettings;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.integTests.framework.annotations.AMetric;
import org.opensearch.performanceanalyzer.rca.integTests.framework.configs.ClusterType;
import org.opensearch.performanceanalyzer.rca.integTests.framework.configs.Consts;
import org.opensearch.performanceanalyzer.rca.integTests.framework.configs.HostTag;
import org.opensearch.performanceanalyzer.threads.ThreadProvider;
import org.opensearch.performanceanalyzer.threads.exceptions.PAThreadException;

public class Cluster {
    // A cluster can have 0 (single node) to 5 (multi node with dedicated cluster_managers) hosts.
    // The
    // following three
    // maps specify what each host will be tagged as.
    private static final Map<Integer, HostTag> hostIdToHostTagMapForDedicatedClusterManager =
            new HashMap<Integer, HostTag>() {
                {
                    put(0, HostTag.ELECTED_CLUSTER_MANAGER);
                    put(1, HostTag.STANDBY_CLUSTER_MANAGER_0);
                    put(2, HostTag.STANDBY_CLUSTER_MANAGER_1);
                    put(3, HostTag.DATA_0);
                    put(4, HostTag.DATA_1);
                }
            };

    private static final Map<Integer, HostTag> hostIdToHostTagMapCoLocatedClusterManager =
            new HashMap<Integer, HostTag>() {
                {
                    put(0, HostTag.ELECTED_CLUSTER_MANAGER);
                    put(1, HostTag.DATA_0);
                }
            };

    private static final HostTag hostIdToHostTagMapSingleNode = HostTag.DATA_0;

    // A queue where exceptions thrown by all the hosts will land.
    private final BlockingQueue<PAThreadException> exceptionQueue;
    private final boolean useHttps;
    private final ClusterType clusterType;

    // The list of all the hosts in the cluster.
    private final List<Host> hostList;

    // The top level directory that will be used by all the hosts for this iteration of the test.
    private final File clusterDir;

    // Same as the thread provide object in PerformanceAnalyzerApp.java
    private final ThreadProvider threadProvider;

    // If you want to get all the hosts that are assigned with role, say data_node, then this is the
    // map
    // to query.
    private final Map<AllMetrics.NodeRole, List<Host>> roleToHostMap;

    private final boolean rcaEnabled;
    private Thread errorHandlingThread;

    // To get a host by a tag. Its the reverse mapping from what the top three maps contain.
    private final Map<HostTag, Host> tagToHostMapping;

    /**
     * @param type The type of cluster - can be dedicated cluster_manager, colocated cluster_manager
     *     or single node.
     * @param clusterDir The directory that will be used by the cluster for files.
     * @param useHttps Should the http and grpc connections use https.
     */
    public Cluster(final ClusterType type, final File clusterDir, final boolean useHttps) {
        this.clusterType = type;
        this.hostList = new ArrayList<>();
        this.roleToHostMap = new HashMap<>();
        this.clusterDir = clusterDir;
        // We start off with the RCA turned off and turn it on only right before we
        // invoke a test method.
        this.rcaEnabled = false;
        this.useHttps = useHttps;
        this.threadProvider = new ThreadProvider();
        this.exceptionQueue = new ArrayBlockingQueue<>(1);
        this.tagToHostMapping = new HashMap<>();

        switch (type) {
            case SINGLE_NODE:
                createSingleNodeCluster();
                break;
            case MULTI_NODE_CO_LOCATED_CLUSTER_MANAGER:
                createMultiNodeCoLocatedClusterManager();
                break;
            case MULTI_NODE_DEDICATED_CLUSTER_MANAGER:
                createMultiNodeDedicatedClusterManager();
                break;
        }

        for (Host host : hostList) {
            host.setClusterDetails(hostList);
        }
    }

    private void createMultiNodeDedicatedClusterManager() {
        int currWebServerPort = PluginSettings.WEBSERVICE_DEFAULT_PORT;
        int currGrpcServerPort = PluginSettings.RPC_DEFAULT_PORT;
        int hostIdx = 0;

        createHost(
                hostIdx,
                AllMetrics.NodeRole.ELECTED_CLUSTER_MANAGER,
                currWebServerPort,
                currGrpcServerPort);

        currWebServerPort += 1;
        currGrpcServerPort += 1;
        hostIdx += 1;

        for (int i = 0; i < Consts.numStandbyClusterManagerNodes; i++) {
            createHost(
                    hostIdx,
                    AllMetrics.NodeRole.CLUSTER_MANAGER,
                    currWebServerPort,
                    currGrpcServerPort);

            currWebServerPort += 1;
            currGrpcServerPort += 1;
            hostIdx += 1;
        }

        for (int i = 0; i < Consts.numDataNodes; i++) {
            createHost(hostIdx, AllMetrics.NodeRole.DATA, currWebServerPort, currGrpcServerPort);

            currWebServerPort += 1;
            currGrpcServerPort += 1;
            hostIdx += 1;
        }
    }

    private Host createHost(
            int hostIdx, AllMetrics.NodeRole role, int webServerPort, int grpcServerPort) {
        HostTag hostTag = getTagForHostIdForHostTagAssignment(hostIdx);
        Host host =
                new Host(
                        hostIdx,
                        useHttps,
                        role,
                        webServerPort,
                        grpcServerPort,
                        this.clusterDir,
                        rcaEnabled,
                        hostTag);
        tagToHostMapping.put(hostTag, host);
        hostList.add(host);

        List<Host> hostByRole = roleToHostMap.get(role);

        if (hostByRole == null) {
            hostByRole = new ArrayList<>();
            hostByRole.add(host);
            roleToHostMap.put(role, hostByRole);
        } else {
            hostByRole.add(host);
        }
        return host;
    }

    private HostTag getTagForHostIdForHostTagAssignment(int hostId) {
        switch (clusterType) {
            case MULTI_NODE_DEDICATED_CLUSTER_MANAGER:
                return hostIdToHostTagMapForDedicatedClusterManager.get(hostId);
            case MULTI_NODE_CO_LOCATED_CLUSTER_MANAGER:
                return hostIdToHostTagMapCoLocatedClusterManager.get(hostId);
            case SINGLE_NODE:
                return hostIdToHostTagMapSingleNode;
        }
        throw new IllegalStateException("No cluster type matches");
    }

    public void createServersAndThreads() {
        this.errorHandlingThread =
                PerformanceAnalyzerApp.startErrorHandlingThread(threadProvider, exceptionQueue);
        for (Host host : hostList) {
            host.createServersAndThreads(threadProvider);
        }
    }

    public void startRcaControllerThread() {
        for (Host host : hostList) {
            host.startRcaControllerThread();
        }
    }

    private void createSingleNodeCluster() {
        int currWebServerPort = PluginSettings.WEBSERVICE_DEFAULT_PORT;
        int currGrpcServerPort = PluginSettings.RPC_DEFAULT_PORT;
        int hostIdx = 0;

        createHost(
                hostIdx,
                AllMetrics.NodeRole.ELECTED_CLUSTER_MANAGER,
                currWebServerPort,
                currGrpcServerPort);
    }

    private void createMultiNodeCoLocatedClusterManager() {
        int currWebServerPort = PluginSettings.WEBSERVICE_DEFAULT_PORT;
        int currGrpcServerPort = PluginSettings.RPC_DEFAULT_PORT;
        int hostIdx = 0;

        createHost(
                hostIdx,
                AllMetrics.NodeRole.ELECTED_CLUSTER_MANAGER,
                currWebServerPort,
                currGrpcServerPort);

        currWebServerPort += 1;
        currGrpcServerPort += 1;
        hostIdx += 1;

        for (int i = 0; i < Consts.numDataNodes - 1; i++) {
            createHost(hostIdx, AllMetrics.NodeRole.DATA, currWebServerPort, currGrpcServerPort);

            currWebServerPort += 1;
            currGrpcServerPort += 1;
            hostIdx += 1;
        }
    }

    public void deleteCluster() throws IOException {
        for (List<Host> hosts : roleToHostMap.values()) {
            for (Host host : hosts) {
                host.deleteHost();
            }
        }
        errorHandlingThread.interrupt();
        deleteClusterDir();
    }

    public void deleteClusterDir() throws IOException {
        for (Host host : hostList) {
            host.deleteHostDir();
        }
        FileUtils.deleteDirectory(clusterDir);
    }

    public void stopRcaScheduler() throws Exception {
        for (Host host : hostList) {
            host.stopRcaScheduler();
        }
    }

    public void startRcaScheduler() throws Exception {
        for (Host host : hostList) {
            host.startRcaScheduler();
        }
    }

    public void updateGraph(final Class rcaGraphClass)
            throws NoSuchMethodException, InstantiationException, IllegalAccessException,
                    InvocationTargetException {
        for (Host host : hostList) {
            host.updateRcaGraph(rcaGraphClass);
        }
    }

    public void updateMetricsDB(AMetric[] metricAnnotations, boolean reloadDB) throws Exception {
        for (Host host : hostList) {
            host.updateMetricsDB(metricAnnotations, reloadDB);
        }
    }

    public JsonElement getAllRcaDataOnHost(HostTag hostTag, String rcaName) {
        return tagToHostMapping.get(hostTag).getDataForRca(rcaName);
    }

    public <T> Object constructObjectFromDBOnHost(HostTag hostTag, Class<T> className)
            throws Exception {
        return tagToHostMapping.get(hostTag).constructObjectFromDB(className);
    }

    public String getRcaRestResponse(
            final String queryUrl, final Map<String, String> params, HostTag hostByTag) {
        return verifyTag(hostByTag).makeRestRequest(queryUrl, params);
    }

    public Map<String, Result<Record>> getRecordsForAllTablesOnHost(HostTag hostTag) {
        return verifyTag(hostTag).getRecordsForAllTables();
    }

    private Host verifyTag(HostTag hostTag) {
        Host host = tagToHostMapping.get(hostTag);
        if (host == null) {
            throw new IllegalArgumentException(
                    "No host with tag '"
                            + hostTag
                            + "' exists. "
                            + "Available tags are: "
                            + tagToHostMapping.keySet());
        }
        return host;
    }
}
