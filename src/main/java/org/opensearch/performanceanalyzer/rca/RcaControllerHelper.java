/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca;


import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.PerformanceAnalyzerApp;
import org.opensearch.performanceanalyzer.metrics.AllMetrics;
import org.opensearch.performanceanalyzer.rca.framework.core.RcaConf;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaRuntimeMetrics;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;

public class RcaControllerHelper {

    private static final Logger LOG = LogManager.getLogger(RcaControllerHelper.class);
    public static final String CAT_CLUSTER_MANAGER_URL =
            "http://localhost:9200/_cat/cluster_manager?h=ip";
    private static String ELECTED_CLUSTER_MANAGER_RCA_CONF_PATH =
            RcaConsts.RCA_CONF_CLUSTER_MANAGER_PATH;
    private static String CLUSTER_MANAGER_RCA_CONF_PATH =
            RcaConsts.RCA_CONF_IDLE_CLUSTER_MANAGER_PATH;
    private static String RCA_CONF_PATH = RcaConsts.RCA_CONF_PATH;

    /**
     * Picks a configuration for RCA based on the node's role.
     *
     * @param nodeRole The role of the node(data/eligible cluster_manager/elected cluster_manager)
     * @return The configuration based on the role.
     */
    public static RcaConf pickRcaConfForRole(final AllMetrics.NodeRole nodeRole) {
        if (AllMetrics.NodeRole.ELECTED_CLUSTER_MANAGER == nodeRole) {
            LOG.debug("picking elected cluster_manager conf");
            return new RcaConf(ELECTED_CLUSTER_MANAGER_RCA_CONF_PATH);
        }

        if (AllMetrics.NodeRole.CLUSTER_MANAGER == nodeRole) {
            LOG.debug("picking idle cluster_manager conf");
            return new RcaConf(CLUSTER_MANAGER_RCA_CONF_PATH);
        }

        if (AllMetrics.NodeRole.DATA == nodeRole) {
            LOG.debug("picking data node conf");
            return new RcaConf(RCA_CONF_PATH);
        }

        LOG.debug("picking default conf");
        return new RcaConf(RCA_CONF_PATH);
    }

    /**
     * Gets the elected cluster_manager's information by performing a _cat/cluster_manager call.
     *
     * @return The host address of the elected cluster_manager.
     */
    public static String getElectedClusterManagerHostAddress() {
        try {
            LOG.info("Making _cat/cluster_manager call");
            PerformanceAnalyzerApp.RCA_RUNTIME_METRICS_AGGREGATOR.updateStat(
                    RcaRuntimeMetrics.OPEN_SEARCH_APIS_CALLED, "catClusterManager", 1);

            final URL url = new URL(CAT_CLUSTER_MANAGER_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine = in.readLine();
            in.close();

            return inputLine;
        } catch (IOException e) {
            LOG.error("Could not get the elected cluster_manager node", e);
        }

        return "";
    }

    /**
     * Builds a thread pool used by the networking layer to pass messages and perform networking
     * functions.
     *
     * @param queueLength The length of the queue in the threadpool.
     * @return The thread pool as an executor service.
     */
    public static ExecutorService buildNetworkThreadPool(final int queueLength) {
        final ThreadFactory rcaNetThreadFactory =
                new ThreadFactoryBuilder()
                        .setNameFormat(RcaConsts.RCA_NETWORK_THREAD_NAME_FORMAT)
                        .setDaemon(true)
                        .build();
        final BlockingQueue<Runnable> threadPoolQueue = new LinkedBlockingQueue<>(queueLength);
        return new ThreadPoolExecutor(
                RcaConsts.NETWORK_CORE_THREAD_COUNT,
                RcaConsts.NETWORK_MAX_THREAD_COUNT,
                0L,
                TimeUnit.MILLISECONDS,
                threadPoolQueue,
                rcaNetThreadFactory);
    }

    public static void set(
            final String rcaConfPath,
            final String rcaMaterConfPath,
            final String rcaElectedClusterManagerConfPath) {
        RCA_CONF_PATH = rcaConfPath;
        CLUSTER_MANAGER_RCA_CONF_PATH = rcaMaterConfPath;
        ELECTED_CLUSTER_MANAGER_RCA_CONF_PATH = rcaElectedClusterManagerConfPath;
    }

    public static List<String> getAllConfFilePaths() {
        return ImmutableList.of(
                ELECTED_CLUSTER_MANAGER_RCA_CONF_PATH,
                CLUSTER_MANAGER_RCA_CONF_PATH,
                RCA_CONF_PATH);
    }
}
