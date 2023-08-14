/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer;


import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sun.net.httpserver.HttpServer;
import io.netty.handler.codec.http.HttpMethod;
import java.net.HttpURLConnection;
import java.util.*;
import java.util.concurrent.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.commons.collectors.*;
import org.opensearch.performanceanalyzer.commons.collectors.StatsCollector;
import org.opensearch.performanceanalyzer.commons.config.ConfigStatus;
import org.opensearch.performanceanalyzer.commons.config.PluginSettings;
import org.opensearch.performanceanalyzer.commons.metrics.MetricsConfiguration;
import org.opensearch.performanceanalyzer.commons.stats.ServiceMetrics;
import org.opensearch.performanceanalyzer.commons.stats.collectors.SampleAggregator;
import org.opensearch.performanceanalyzer.commons.stats.emitters.ISampler;
import org.opensearch.performanceanalyzer.commons.stats.emitters.PeriodicSamplers;
import org.opensearch.performanceanalyzer.commons.stats.listeners.IListener;
import org.opensearch.performanceanalyzer.commons.stats.measurements.MeasurementSet;
import org.opensearch.performanceanalyzer.commons.stats.metrics.StatExceptionCode;
import org.opensearch.performanceanalyzer.config.TroubleshootingConfig;
import org.opensearch.performanceanalyzer.core.Util;
import org.opensearch.performanceanalyzer.metrics.MetricsRestUtil;
import org.opensearch.performanceanalyzer.metrics.handler.MetricsServerHandler;
import org.opensearch.performanceanalyzer.net.GRPCConnectionManager;
import org.opensearch.performanceanalyzer.net.NetClient;
import org.opensearch.performanceanalyzer.net.NetServer;
import org.opensearch.performanceanalyzer.rca.RcaController;
import org.opensearch.performanceanalyzer.rca.framework.core.MetricsDBProvider;
import org.opensearch.performanceanalyzer.rca.framework.metrics.*;
import org.opensearch.performanceanalyzer.rca.framework.sys.AllJvmSamplers;
import org.opensearch.performanceanalyzer.rca.framework.util.RcaConsts;
import org.opensearch.performanceanalyzer.rca.listener.MisbehavingGraphOperateMethodListener;
import org.opensearch.performanceanalyzer.rca.samplers.BatchMetricsEnabledSampler;
import org.opensearch.performanceanalyzer.rca.samplers.MetricsDBFileSampler;
import org.opensearch.performanceanalyzer.rca.samplers.RcaStateSamplers;
import org.opensearch.performanceanalyzer.reader.ReaderMetricsProcessor;
import org.opensearch.performanceanalyzer.rest.QueryBatchRequestHandler;
import org.opensearch.performanceanalyzer.rest.QueryMetricsRequestHandler;
import org.opensearch.performanceanalyzer.threads.ThreadProvider;
import org.opensearch.performanceanalyzer.threads.exceptions.PAThreadException;

public class PerformanceAnalyzerApp {

    private static final Logger LOG = LogManager.getLogger(PerformanceAnalyzerApp.class);

    public static final int READER_RESTART_MAX_ATTEMPTS = 3;
    private static final int EXCEPTION_QUEUE_LENGTH = 1;
    private static final ScheduledMetricCollectorsExecutor METRIC_COLLECTOR_EXECUTOR =
            new ScheduledMetricCollectorsExecutor(1, false);
    private static final ScheduledExecutorService netOperationsExecutor =
            Executors.newScheduledThreadPool(
                    2, new ThreadFactoryBuilder().setNameFormat("network-thread-%d").build());

    private static RcaController rcaController = null;
    private static final ThreadProvider THREAD_PROVIDER = new ThreadProvider();

    public static void initAggregators() {
        ServiceMetrics.READER_METRICS_AGGREGATOR = new SampleAggregator(ReaderMetrics.values());
        ServiceMetrics.RCA_GRAPH_METRICS_AGGREGATOR =
                new SampleAggregator(RcaGraphMetrics.values());
        ServiceMetrics.RCA_RUNTIME_METRICS_AGGREGATOR =
                new SampleAggregator(RcaRuntimeMetrics.values());
        ServiceMetrics.RCA_VERTICES_METRICS_AGGREGATOR =
                new SampleAggregator(RcaVerticesMetrics.values());
        final IListener MISBEHAVING_NODES_LISTENER = new MisbehavingGraphOperateMethodListener();
        ServiceMetrics.ERRORS_AND_EXCEPTIONS_AGGREGATOR =
                new SampleAggregator(
                        MISBEHAVING_NODES_LISTENER.getMeasurementsListenedTo(),
                        MISBEHAVING_NODES_LISTENER,
                        ExceptionsAndErrors.values());
        ServiceMetrics.PERIODIC_SAMPLE_AGGREGATOR =
                new SampleAggregator(getPeriodicMeasurementSets());
        ServiceMetrics.initStatsReporter();
    }

    static {
        initAggregators();
        Objects.requireNonNull(
                ServiceMetrics.STATS_REPORTER, "Service Metrics(Stat) Reporter should not be null");
    }

    public static PeriodicSamplers PERIODIC_SAMPLERS;
    public static final BlockingQueue<PAThreadException> exceptionQueue =
            new ArrayBlockingQueue<>(EXCEPTION_QUEUE_LENGTH);

    public static void main(String[] args) {
        StatsCollector.STATS_TYPE = "agent-stats-metadata";
        PluginSettings settings = PluginSettings.instance();
        if (ConfigStatus.INSTANCE.haveValidConfig()) {
            AppContext appContext = new AppContext();
            PERIODIC_SAMPLERS =
                    new PeriodicSamplers(
                            ServiceMetrics.PERIODIC_SAMPLE_AGGREGATOR,
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
            StatsCollector.instance()
                    .logException(StatExceptionCode.INVALID_CONFIG_RCA_AGENT_STOPPED);
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
                                    LOG.error(
                                            "Exception handling thread interrupted. Reason: {}",
                                            e.getMessage(),
                                            e);
                                    StatsCollector.instance()
                                            .logException(
                                                    StatExceptionCode.ERROR_HANDLER_THREAD_STOPPED);
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
        // Currently, this will only log an exception and increment a metric indicating that the
        // thread has died.
        // As an improvement to this functionality, once we know what exceptions are retryable, we
        // can have each thread also register an error handler for itself. This handler will know
        // what to do when the thread has stopped due to an unexpected exception.
        LOG.error(
                "Thread: {} ran into an uncaught exception: {}",
                exception.getPaThreadName(),
                exception.getInnerThrowable(),
                exception);
        StatsCollector.instance().logException(StatExceptionCode.READER_METRICS_PROCESSOR_ERROR);
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

    public static void startReaderThread(
            final AppContext appContext, final ThreadProvider threadProvider) {
        PluginSettings settings = PluginSettings.instance();
        final Thread readerThread =
                threadProvider.createThreadForRunnable(
                        () -> {
                            int retryAttemptLeft = READER_RESTART_MAX_ATTEMPTS;
                            while (retryAttemptLeft > 0) {
                                try {
                                    ReaderMetricsProcessor mp =
                                            new ReaderMetricsProcessor(
                                                    settings.getMetricsLocation(),
                                                    true,
                                                    appContext);
                                    ReaderMetricsProcessor.setCurrentInstance(mp);
                                    mp.run();
                                } catch (Throwable e) {
                                    retryAttemptLeft--;
                                    LOG.error(
                                            "Error in ReaderMetricsProcessor...restarting, retryCount left: {}."
                                                    + "Exception: {}",
                                            retryAttemptLeft,
                                            e.getMessage());
                                    StatsCollector.instance()
                                            .logException(
                                                    StatExceptionCode.READER_RESTART_PROCESSING);

                                    if (TroubleshootingConfig.getEnableDevAssert()) break;

                                    // All retry attempts exhausted; handle thread failure
                                    if (retryAttemptLeft <= 0) handleReaderThreadFailed();
                                }
                            }
                        },
                        PerformanceAnalyzerThreads.PA_READER);
        readerThread.start();
    }

    private static void handleReaderThreadFailed() {
        // Reader subcomponent is responsible for processing, cleaning metrics written by PA.
        // Since Reader thread fails to start successfully, execute following:
        //
        // 1. Disable PA - Stop collecting OpenSearch metrics
        // 2. Terminate RCA Process - Gracefully shutdown all
        //    existing resources/channels, including Reader.
        try {
            LOG.info(
                    "Exhausted {} attempts - unable to start Reader Thread successfully; disable PA",
                    READER_RESTART_MAX_ATTEMPTS);
            disablePA();
            LOG.info("Attempt to disable PA succeeded.");
            StatsCollector.instance()
                    .logException(StatExceptionCode.READER_ERROR_PA_DISABLE_SUCCESS);
        } catch (Throwable e) {
            LOG.info("Attempt to disable PA failing: {}", e.getMessage());
            StatsCollector.instance()
                    .logException(StatExceptionCode.READER_ERROR_PA_DISABLE_FAILED);
        } finally {
            cleanupAndExit();
        }
    }

    private static void disablePA() {
        String PA_CONFIG_PATH = Util.PA_BASE_URL + "/cluster/config";
        String PA_DISABLE_PAYLOAD = "{\"enabled\": false}";

        int resCode =
                ESLocalhostConnection.makeHttpRequest(
                        PA_CONFIG_PATH, HttpMethod.POST, PA_DISABLE_PAYLOAD);
        if (resCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed to disable PA");
        }
    }

    private static void cleanupAndExit() {
        LOG.info("Reader thread not coming up successfully - Shutting down RCA Runtime");
        StatsCollector.instance().logException(StatExceptionCode.READER_ERROR_RCA_AGENT_STOPPED);

        // Terminate Java Runtime, executes {@link #shutDownGracefully(ClientServers clientServers)}
        System.exit(1);
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
