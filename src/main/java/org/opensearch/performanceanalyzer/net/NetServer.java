/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.net;

import com.google.common.annotations.VisibleForTesting;
import io.grpc.Server;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.CertificateUtils;
import org.opensearch.performanceanalyzer.commons.collectors.StatsCollector;
import org.opensearch.performanceanalyzer.commons.stats.metrics.StatExceptionCode;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.grpc.InterNodeRpcServiceGrpc;
import org.opensearch.performanceanalyzer.grpc.MetricsRequest;
import org.opensearch.performanceanalyzer.grpc.MetricsResponse;
import org.opensearch.performanceanalyzer.grpc.PublishResponse;
import org.opensearch.performanceanalyzer.grpc.SubscribeMessage;
import org.opensearch.performanceanalyzer.grpc.SubscribeResponse;
import org.opensearch.performanceanalyzer.metrics.handler.MetricsServerHandler;
import org.opensearch.performanceanalyzer.rca.net.handler.PublishRequestHandler;
import org.opensearch.performanceanalyzer.rca.net.handler.SubscribeServerHandler;

/** Class that runs the RPC server and implements the RPC methods. */
public class NetServer extends InterNodeRpcServiceGrpc.InterNodeRpcServiceImplBase
        implements Runnable {

    private static final Logger LOG = LogManager.getLogger(NetServer.class);

    /** The RPC server port. */
    private final int port;

    /** Number of threads to be used by the server. */
    private final int numServerThreads;

    /** Flag indicating if a secure channel is to be used or otherwise. */
    private final boolean useHttps;

    /** Handler implementing publish RPC. */
    private PublishRequestHandler sendDataHandler;

    /** Handler implementing the subscribe RPC. */
    private SubscribeServerHandler subscribeHandler;

    /** Handler implementing the metric RPC for retrieving Performance Analyzer metrics. */
    private MetricsServerHandler metricsServerHandler;

    /** The server instance. */
    protected Server server;

    private volatile boolean attemptedShutdown;

    public NetServer(final int port, final int numServerThreads, final boolean useHttps) {
        this.port = port;
        this.numServerThreads = numServerThreads;
        this.useHttps = useHttps;
        this.attemptedShutdown = false;
    }

    // postStartHook executes after the NetServer has successfully started its Server
    protected void postStartHook() {}

    // shutdownHook executes after the NetServer has shutdown its Server
    protected void shutdownHook() {}

    /**
     * When an object implementing interface <code>Runnable</code> is used to create a thread,
     * starting the thread causes the object's <code>run</code> method to be called in that
     * separately executing thread.
     *
     * <p>The general contract of the method <code>run</code> is that it may take any action
     * whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        LOG.info(
                "Starting the gRPC server on port {} with {} threads. Using HTTPS: {}",
                port,
                numServerThreads,
                useHttps);
        try {
            if (useHttps) {
                server =
                        buildHttpsServer(
                                CertificateUtils.getTrustedCasFile(),
                                CertificateUtils.getCertificateFile(),
                                CertificateUtils.getPrivateKeyFile());
            } else {
                server = buildHttpServer();
            }
            server.start();
            LOG.info("gRPC server started successfully!");
            postStartHook();
            server.awaitTermination();
            LOG.info("gRPC server terminating..");
        } catch (InterruptedException | IOException e) {
            if (!this.attemptedShutdown) {
                // print stack trace only if this wasn't meant to be.
                LOG.error("GrpcServer interrupted", e);
            }
            server.shutdownNow();
            shutdownHook();
        }
    }

    private NettyServerBuilder buildBaseServer() {
        return NettyServerBuilder.forPort(port)
                .addService(this)
                .bossEventLoopGroup(new NioEventLoopGroup(numServerThreads))
                .workerEventLoopGroup(new NioEventLoopGroup(numServerThreads))
                .channelType(NioServerSocketChannel.class);
    }

    private Server buildHttpServer() {
        return buildBaseServer().executor(Executors.newSingleThreadExecutor()).build();
    }

    protected Server buildHttpsServer(File trustedCasFile, File certFile, File pkeyFile)
            throws SSLException {
        SslContextBuilder sslContextBuilder = GrpcSslContexts.forServer(certFile, pkeyFile);
        // If an authority is specified, authenticate clients
        if (trustedCasFile != null) {
            sslContextBuilder.trustManager(trustedCasFile).clientAuth(ClientAuth.REQUIRE);
        }
        return buildBaseServer().sslContext(sslContextBuilder.build()).build();
    }

    /**
     *
     *
     * <pre>
     * Sends a flowunit to whoever is interested in it.
     * </pre>
     *
     * @param responseObserver The response stream.
     */
    @Override
    public StreamObserver<FlowUnitMessage> publish(
            final StreamObserver<PublishResponse> responseObserver) {
        LOG.debug("publish received");
        if (sendDataHandler != null) {
            return sendDataHandler.getClientStream(responseObserver);
        }

        throw new UnsupportedOperationException("No rpc handler found for publish/");
    }

    /**
     *
     *
     * <pre>
     * Sends a subscription request to a node for a particular metric.
     * </pre>
     *
     * @param request The subscribe request.
     * @param responseObserver The response stream to which subscription status is written to.
     */
    @Override
    public void subscribe(
            final SubscribeMessage request,
            final StreamObserver<SubscribeResponse> responseObserver) {
        if (subscribeHandler != null) {
            subscribeHandler.handleSubscriptionRequest(request, responseObserver);
        } else {
            LOG.error("Subscribe request received before handler is set.");
            responseObserver.onError(
                    new UnsupportedOperationException("No rpc handler found for " + "subscribe/"));
        }
    }

    @Override
    public void getMetrics(
            MetricsRequest request, StreamObserver<MetricsResponse> responseObserver) {
        if (metricsServerHandler != null) {
            metricsServerHandler.collectAPIData(request, responseObserver);
        }
    }

    public void setSubscribeHandler(SubscribeServerHandler subscribeHandler) {
        this.subscribeHandler = subscribeHandler;
    }

    public void setSendDataHandler(PublishRequestHandler sendDataHandler) {
        this.sendDataHandler = sendDataHandler;
    }

    public void setMetricsHandler(MetricsServerHandler metricsServerHandler) {
        this.metricsServerHandler = metricsServerHandler;
    }

    /**
     * Unit test usage only.
     *
     * @return Current handler for /metrics rpc.
     */
    @VisibleForTesting
    public MetricsServerHandler getMetricsServerHandler() {
        return metricsServerHandler;
    }

    /**
     * Unit test usage only.
     *
     * @return Current handler for /publish rpc.
     */
    @VisibleForTesting
    public PublishRequestHandler getSendDataHandler() {
        return sendDataHandler;
    }

    /**
     * Unit test usage only.
     *
     * @return Current handler for /subscribe rpc.
     */
    @VisibleForTesting
    public SubscribeServerHandler getSubscribeHandler() {
        return subscribeHandler;
    }

    public void stop() {
        LOG.debug("indicating upstream nodes that current node is going down..");
        if (sendDataHandler != null) {
            sendDataHandler.terminateUpstreamConnections();
        }

        // Remove handlers.
        sendDataHandler = null;
        subscribeHandler = null;
    }

    public void shutdown() {
        stop();
        // Actually stop the server
        if (server != null) {
            server.shutdown();
            try {
                if (!server.awaitTermination(1, TimeUnit.MINUTES)) {
                    StatsCollector.instance()
                            .logException(StatExceptionCode.GRPC_SERVER_CLOSURE_ERROR);
                    LOG.warn("Timed out while gracefully shutting down net server");
                }
            } catch (InterruptedException e) {
                server.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void setAttemptedShutdown() {
        attemptedShutdown = true;
    }
}
