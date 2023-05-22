/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer;


import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.net.httpserver.HttpServer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.collectors.*;
import org.opensearch.performanceanalyzer.commons.collectors.StatExceptionCode;
import org.opensearch.performanceanalyzer.commons.collectors.StatsCollector;
import org.opensearch.performanceanalyzer.commons.metrics.ExceptionsAndErrors;
import org.opensearch.performanceanalyzer.commons.metrics.MeasurementSet;
import org.opensearch.performanceanalyzer.commons.metrics.MetricsConfiguration;
import org.opensearch.performanceanalyzer.commons.stats.CommonStats;
import org.opensearch.performanceanalyzer.config.PluginSettings;
import org.opensearch.performanceanalyzer.config.TroubleshootingConfig;
import org.opensearch.performanceanalyzer.core.Util;
import org.opensearch.performanceanalyzer.jvm.GCMetrics;
import org.opensearch.performanceanalyzer.jvm.HeapMetrics;
import org.opensearch.performanceanalyzer.jvm.ThreadList;
import org.opensearch.performanceanalyzer.metrics.MetricsRestUtil;
import org.opensearch.performanceanalyzer.metrics.handler.MetricsServerHandler;
import org.opensearch.performanceanalyzer.net.GRPCConnectionManager;
import org.opensearch.performanceanalyzer.net.NetClient;
import org.opensearch.performanceanalyzer.net.NetServer;
import org.opensearch.performanceanalyzer.os.OSGlobals;
import org.opensearch.performanceanalyzer.os.ThreadCPU;
import org.opensearch.performanceanalyzer.os.ThreadDiskIO;
import org.opensearch.performanceanalyzer.os.ThreadSched;
import org.opensearch.performanceanalyzer.rca.RcaController;
import org.opensearch.performanceanalyzer.rca.framework.core.MetricsDBProvider;
import org.opensearch.performanceanalyzer.rca.framework.metrics.JvmMetrics;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaRuntimeMetrics;
import org.opensearch.performanceanalyzer.rca.framework.metrics.ReaderMetrics;
import org.opensearch.performanceanalyzer.rca.framework.sys.AllJvmSamplers;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;
import org.opensearch.performanceanalyzer.rca.samplers.BatchMetricsEnabledSampler;
import org.opensearch.performanceanalyzer.rca.samplers.MetricsDBFileSampler;
import org.opensearch.performanceanalyzer.rca.samplers.RcaStateSamplers;
import org.opensearch.performanceanalyzer.rca.stats.emitters.ISampler;
import org.opensearch.performanceanalyzer.rca.stats.emitters.PeriodicSamplers;
import org.opensearch.performanceanalyzer.reader.ReaderMetricsProcessor;
import org.opensearch.performanceanalyzer.rest.QueryBatchRequestHandler;
import org.opensearch.performanceanalyzer.rest.QueryMetricsRequestHandler;
import org.opensearch.performanceanalyzer.threads.ThreadProvider;
import org.opensearch.performanceanalyzer.threads.exceptions.PAThreadException;

public class PerformanceAnalyzerApp {

    private static final int EXCEPTION_QUEUE_LENGTH = 1;
    private static final Logger LOG = LogManager.getLogger(PerformanceAnalyzerApp.class);
    private static final ScheduledMetricCollectorsExecutor METRIC_COLLECTOR_EXECUTOR =
            new ScheduledMetricCollectorsExecutor(1, false);
    private static final ScheduledExecutorService netOperationsExecutor =
            Executors.newScheduledThreadPool(
                    2, new ThreadFactoryBuilder().setNameFormat("network-thread-%d").build());

    private static RcaController rcaController = null;
    private static final ThreadProvider THREAD_PROVIDER = new ThreadProvider();

    static {
        System.out.println("start");
        //        CommonStats.RCA_GRAPH_METRICS_AGGREGATOR =
        //                new SampleAggregator(RcaGraphMetrics.values());
        //        CommonStats.RCA_RUNTIME_METRICS_AGGREGATOR =
        //                new SampleAggregator(RcaRuntimeMetrics.values());
        //        CommonStats.RCA_VERTICES_METRICS_AGGREGATOR =
        //                new SampleAggregator(RcaVerticesMetrics.values());
        //        CommonStats.READER_METRICS_AGGREGATOR =
        //                new SampleAggregator(ReaderMetrics.values());
        //        CommonStats.WRITER_METRICS_AGGREGATOR =
        //                new SampleAggregator(WriterMetrics.values());
        //
        //        CommonStats.MISBEHAVING_NODES_LISTENER =
        //                new MisbehavingGraphOperateMethodListener();
        //        CommonStats.ERRORS_AND_EXCEPTIONS_AGGREGATOR =
        //                new SampleAggregator(
        //                        MISBEHAVING_NODES_LISTENER.getMeasurementsListenedTo(),
        //                        MISBEHAVING_NODES_LISTENER,
        //                        ExceptionsAndErrors.values());
        //
        //        CommonStats.PERIODIC_SAMPLE_AGGREGATOR =
        //                new SampleAggregator(getPeriodicMeasurementSets());
        //
        //        CommonStats.RCA_STATS_REPORTER =
        //                new RcaStatsReporter(
        //                        Arrays.asList(
        //                                RCA_GRAPH_METRICS_AGGREGATOR,
        //                                RCA_RUNTIME_METRICS_AGGREGATOR,
        //                                RCA_VERTICES_METRICS_AGGREGATOR,
        //                                READER_METRICS_AGGREGATOR,
        //                                WRITER_METRICS_AGGREGATOR,
        //                                ERRORS_AND_EXCEPTIONS_AGGREGATOR,
        //                                PERIODIC_SAMPLE_AGGREGATOR));
    }

    public static PeriodicSamplers PERIODIC_SAMPLERS;
    public static final BlockingQueue<PAThreadException> exceptionQueue =
            new ArrayBlockingQueue<>(EXCEPTION_QUEUE_LENGTH);

    public static void initMetricsConfig() {
        final MetricsConfiguration.MetricConfig defaultConfig = MetricsConfiguration.cdefault;
        final Map<Class, MetricsConfiguration.MetricConfig> configMap =
                MetricsConfiguration.CONFIG_MAP;

        configMap.put(ThreadCPU.class, defaultConfig);
        configMap.put(ThreadDiskIO.class, defaultConfig);
        configMap.put(ThreadSched.class, defaultConfig);
        configMap.put(ThreadList.class, defaultConfig);
        configMap.put(GCMetrics.class, defaultConfig);
        configMap.put(HeapMetrics.class, defaultConfig);
        configMap.put(NetworkE2ECollector.class, defaultConfig);
        configMap.put(NetworkInterfaceCollector.class, defaultConfig);
        configMap.put(OSGlobals.class, defaultConfig);
        configMap.put(
                StatsCollector.class,
                new MetricsConfiguration.MetricConfig(
                        MetricsConfiguration.STATS_ROTATION_INTERVAL, 0));
        configMap.put(DisksCollector.class, defaultConfig);
        configMap.put(HeapMetricsCollector.class, defaultConfig);
        configMap.put(GCInfoCollector.class, defaultConfig);
        configMap.put(MountedPartitionMetricsCollector.class, defaultConfig);
    }

    static {
        initMetricsConfig();
    }

    public static void main(String[] args) {
        StatsCollector.STATS_TYPE = "agent-stats-metadata";
        PluginSettings settings = PluginSettings.instance();
        if (ConfigStatus.INSTANCE.haveValidConfig()) {
            AppContext appContext = new AppContext();
            PERIODIC_SAMPLERS =
                    new PeriodicSamplers(
                            CommonStats.PERIODIC_SAMPLE_AGGREGATOR,
                            getAllSamplers(appContext),
                            (MetricsConfiguration.CONFIG_MAP.get(StatsCollector.class)
                                            .samplingInterval)
                                    / 2,
                            TimeUnit.MILLISECONDS);
            METRIC_COLLECTOR_EXECUTOR.addScheduledMetricCollector(StatsCollector.instance());
            METRIC_COLLECTOR_EXECUTOR.setEnabled(true);
            METRIC_COLLECTOR_EXECUTOR.start();

            final GRPCConnectionManager connectionManager =
                    new GRPCConnectionManager(settings.getHttpsEnabled());
            final ClientServers clientServers = createClientServers(connectionManager, appContext);

            addShutdownHook(clientServers);
            startErrorHandlingThread(THREAD_PROVIDER, exceptionQueue);

            startReaderThread(appContext, THREAD_PROVIDER);
            startGrpcServerThread(clientServers.getNetServer(), THREAD_PROVIDER);
            startWebServerThread(clientServers.getHttpServer(), THREAD_PROVIDER);
            startRcaTopLevelThread(clientServers, connectionManager, appContext, THREAD_PROVIDER);
        } else {
            LOG.error("Performance analyzer app stopped due to invalid config status.");
            CommonStats.ERRORS_AND_EXCEPTIONS_AGGREGATOR.updateStat(
                    ExceptionsAndErrors.INVALID_CONFIG_RCA_AGENT_STOPPED, "", 1);
        }
    }

    private static void startRcaTopLevelThread(
            final ClientServers clientServers,
            final GRPCConnectionManager connectionManager,
            final AppContext appContext,
            final ThreadProvider threadProvider) {
        rcaController =
                new RcaController(
                        threadProvider,
                        netOperationsExecutor,
                        connectionManager,
                        clientServers,
                        Util.DATA_DIR,
                        RcaConsts.RCA_STATE_CHECK_INTERVAL_IN_MS,
                        RcaConsts.nodeRolePollerPeriodicityInSeconds * 1000,
                        appContext,
                        new MetricsDBProvider());
        startRcaTopLevelThread(rcaController, threadProvider);
    }

    public static Thread startRcaTopLevelThread(
            final RcaController rcaController1, final ThreadProvider threadProvider) {
        return startRcaTopLevelThread(rcaController1, threadProvider, "");
    }

    public static Thread startRcaTopLevelThread(
            final RcaController rcaController1,
            final ThreadProvider threadProvider,
            String nodeName) {
        Thread rcaControllerThread =
                threadProvider.createThreadForRunnable(
                        () -> rcaController1.run(),
                        PerformanceAnalyzerThreads.RCA_CONTROLLER,
                        nodeName);
        rcaControllerThread.start();
        return rcaControllerThread;
    }

    public static Thread startErrorHandlingThread(
            final ThreadProvider threadProvider,
            final BlockingQueue<PAThreadException> errorQueue) {
        final Thread errorHandlingThread =
                threadProvider.createThreadForRunnable(
                        () -> {
                            while (true) {
                                try {
                                    final PAThreadException exception = errorQueue.take();
                                    handle(exception);
                                } catch (InterruptedException e) {
                                    CommonStats.READER_METRICS_AGGREGATOR.updateStat(
                                            ReaderMetrics.ERROR_HANDLER_THREAD_STOPPED, "", 1);
                                    LOG.error(
                                            "Exception handling thread interrupted. Reason: {}",
                                            e.getMessage(),
                                            e);
                                    break;
                                }
                            }
                        },
                        PerformanceAnalyzerThreads.PA_ERROR_HANDLER);

        errorHandlingThread.start();
        return errorHandlingThread;
    }

    /**
     * Handles any exception thrown from the threads which are not handled by the thread itself.
     *
     * @param exception The exception thrown from the thread.
     */
    private static void handle(PAThreadException exception) {
        // Currently this will only log an exception and increment a metric indicating that the
        // thread has died.
        // As an improvement to this functionality, once we know what exceptions are retryable, we
        // can have each thread also register an error handler for itself. This handler will know
        // what to do when the thread has stopped due to an unexpected exception.
        CommonStats.READER_METRICS_AGGREGATOR.updateStat(ReaderMetrics.OTHER, "", 1);
        LOG.error(
                "Thread: {} ran into an uncaught exception: {}",
                exception.getPaThreadName(),
                exception.getInnerThrowable(),
                exception);
    }

    public static Thread startWebServerThread(
            final HttpServer server, final ThreadProvider threadProvider) {
        final Thread webServerThread =
                threadProvider.createThreadForRunnable(
                        server::start, PerformanceAnalyzerThreads.WEB_SERVER);
        // We don't want to hold up the app from restarting just because the web server is up and
        // all
        // other threads have died.
        webServerThread.setDaemon(true);
        webServerThread.start();
        return webServerThread;
    }

    public static Thread startGrpcServerThread(
            final NetServer server, final ThreadProvider threadProvider) {
        final Thread grpcServerThread =
                threadProvider.createThreadForRunnable(
                        server, PerformanceAnalyzerThreads.GRPC_SERVER);
        // We don't want to hold up the app from restarting just because the grpc server is up and
        // all other threads have died.
        grpcServerThread.setDaemon(true);
        grpcServerThread.start();
        return grpcServerThread;
    }

    private static void startReaderThread(
            final AppContext appContext, final ThreadProvider threadProvider) {
        PluginSettings settings = PluginSettings.instance();
        final Thread readerThread =
                threadProvider.createThreadForRunnable(
                        () -> {
                            while (true) {
                                try {
                                    ReaderMetricsProcessor mp =
                                            new ReaderMetricsProcessor(
                                                    settings.getMetricsLocation(),
                                                    true,
                                                    appContext);
                                    ReaderMetricsProcessor.setCurrentInstance(mp);
                                    mp.run();
                                } catch (Throwable e) {
                                    if (TroubleshootingConfig.getEnableDevAssert()) {
                                        break;
                                    }
                                    LOG.error(
                                            "Error in ReaderMetricsProcessor...restarting, ExceptionCode: {}",
                                            StatExceptionCode.READER_RESTART_PROCESSING.toString());
                                    StatsCollector.instance()
                                            .logException(
                                                    StatExceptionCode.READER_RESTART_PROCESSING);
                                }
                            }
                        },
                        PerformanceAnalyzerThreads.PA_READER);

        readerThread.start();
    }

    /**
     * Start all the servers and clients for request processing. We start two servers: - httpServer:
     * To handle the curl requests sent to the endpoint. This is human readable and also used by the
     * perftop. - gRPC server: This is how metrics, RCAs etc are transported between nodes. and a
     * gRPC client.
     *
     * @return gRPC client and the gRPC server and the httpServer wrapped in a class.
     */
    public static ClientServers createClientServers(
            final GRPCConnectionManager connectionManager, final AppContext appContext) {
        PluginSettings settings = PluginSettings.instance();
        boolean useHttps = settings.getHttpsEnabled();
        return createClientServers(
                connectionManager,
                settings.getRpcPort(),
                new MetricsServerHandler(),
                new MetricsRestUtil(),
                useHttps,
                settings.getWebServicePort(),
                settings.getSettingValue(PerformanceAnalyzerWebServer.WEBSERVICE_BIND_HOST_NAME),
                appContext);
    }

    public static ClientServers createClientServers(
            final GRPCConnectionManager connectionManager,
            int rpcPort,
            final MetricsServerHandler metricsServerHandler,
            final MetricsRestUtil metricsRestUtil,
            boolean useHttps,
            int webServerPort,
            final String hostFromSetting,
            final AppContext appContext) {
        NetServer netServer = new NetServer(rpcPort, 1, useHttps);
        NetClient netClient = new NetClient(connectionManager);

        if (metricsServerHandler != null) {
            netServer.setMetricsHandler(metricsServerHandler);
        }

        HttpServer httpServer =
                PerformanceAnalyzerWebServer.createInternalServer(
                        webServerPort, hostFromSetting, useHttps);

        if (metricsRestUtil != null) {
            QueryMetricsRequestHandler queryMetricsRequestHandler =
                    new QueryMetricsRequestHandler(netClient, metricsRestUtil, appContext);
            httpServer.createContext(Util.METRICS_QUERY_URL, queryMetricsRequestHandler);
            httpServer.createContext(
                    Util.LEGACY_OPENDISTRO_METRICS_QUERY_URL, queryMetricsRequestHandler);

            QueryBatchRequestHandler queryBatchRequestHandler =
                    new QueryBatchRequestHandler(netClient, metricsRestUtil);
            httpServer.createContext(Util.BATCH_METRICS_URL, queryBatchRequestHandler);
            httpServer.createContext(
                    Util.LEGACY_OPENDISTRO_BATCH_METRICS_URL, queryBatchRequestHandler);
        }

        return new ClientServers(httpServer, netServer, netClient);
    }

    public static List<ISampler> getAllSamplers(final AppContext appContext) {
        List<ISampler> allSamplers = new ArrayList<>();
        allSamplers.addAll(AllJvmSamplers.getJvmSamplers());
        allSamplers.add(RcaStateSamplers.getRcaEnabledSampler(appContext));
        allSamplers.add(new BatchMetricsEnabledSampler(appContext));
        allSamplers.add(new MetricsDBFileSampler(appContext));

        return allSamplers;
    }

    private static MeasurementSet[] getPeriodicMeasurementSets() {
        List<MeasurementSet> measurementSets = new ArrayList<>();
        measurementSets.addAll(Arrays.asList(JvmMetrics.values()));
        measurementSets.add(RcaRuntimeMetrics.RCA_ENABLED);
        measurementSets.add(ReaderMetrics.BATCH_METRICS_ENABLED);
        measurementSets.add(ReaderMetrics.METRICSDB_NUM_FILES);
        measurementSets.add(ReaderMetrics.METRICSDB_SIZE_FILES);
        measurementSets.add(ReaderMetrics.METRICSDB_NUM_UNCOMPRESSED_FILES);
        measurementSets.add(ReaderMetrics.METRICSDB_SIZE_UNCOMPRESSED_FILES);

        return measurementSets.toArray(new MeasurementSet[] {});
    }

    public static RcaController getRcaController() {
        return rcaController;
    }

    @VisibleForTesting
    public static void setRcaController(RcaController rcaController) {
        PerformanceAnalyzerApp.rcaController = rcaController;
    }

    // Adds a hook to shut down resources after PA process exits due to some reason.
    private static void addShutdownHook(ClientServers clientServers) {
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    LOG.info("Trying to shutdown performance analyzer gracefully");
                                    shutDownGracefully(clientServers);
                                }));
    }

    /**
     * Shuts down all the resources/channels gracefully which were created initially.
     *
     * @param clientServers contains all the server created by the app.
     */
    private static void shutDownGracefully(ClientServers clientServers) {
        rcaController.stop();
        clientServers.getNetServer().shutdown();
        clientServers.getHttpServer().stop(3);
        ReaderMetricsProcessor.getInstance().shutdown();
    }
}
