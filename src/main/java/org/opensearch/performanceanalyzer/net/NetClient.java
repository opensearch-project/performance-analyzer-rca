/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.net;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.performanceanalyzer.commons.collectors.StatsCollector;
import org.opensearch.performanceanalyzer.commons.stats.ServiceMetrics;
import org.opensearch.performanceanalyzer.commons.stats.metrics.StatExceptionCode;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.grpc.InterNodeRpcServiceGrpc;
import org.opensearch.performanceanalyzer.grpc.MetricsRequest;
import org.opensearch.performanceanalyzer.grpc.MetricsResponse;
import org.opensearch.performanceanalyzer.grpc.PublishResponse;
import org.opensearch.performanceanalyzer.grpc.SubscribeMessage;
import org.opensearch.performanceanalyzer.grpc.SubscribeResponse;
import org.opensearch.performanceanalyzer.rca.framework.metrics.RcaGraphMetrics;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;

/**
 * This class aims to abstract out managing client connections to the server and other boilerplate
 * stuff.
 */
public class NetClient {

    private static final Logger LOG = LogManager.getLogger(NetClient.class);

    /** The connection manager instance that holds objects needed to make RPCs. */
    private final GRPCConnectionManager connectionManager;

    public NetClient(final GRPCConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public GRPCConnectionManager getConnectionManager() {
        return connectionManager;
    }

    private ConcurrentMap<
                    InstanceDetails.Id,
                    ConcurrentMap<String, AtomicReference<StreamObserver<FlowUnitMessage>>>>
            perHostAndNodeOpenDataStreamMap = new ConcurrentHashMap<>();

    // Visible for testing
    protected ConcurrentMap<
                    InstanceDetails.Id,
                    ConcurrentMap<String, AtomicReference<StreamObserver<FlowUnitMessage>>>>
            getPerHostAndNodeOpenDataStreamMap() {
        return perHostAndNodeOpenDataStreamMap;
    }

    /**
     * Sends a subscribe request to a remote host. If the subscribe request fails because the remote
     * host is not ready/encountered an exception, we still retry subscribing when we try reading
     * from remote hosts during graph execution.
     *
     * @param remoteHost The host that the subscribe request is for.
     * @param subscribeMessage The subscribe protobuf message.
     * @param serverResponseStream The response stream for the server to communicate back on.
     */
    public void subscribe(
            final InstanceDetails remoteHost,
            final SubscribeMessage subscribeMessage,
            StreamObserver<SubscribeResponse> serverResponseStream) {
        LOG.debug("Trying to send intent message to {}", remoteHost);
        try {
            connectionManager
                    .getClientStubForHost(remoteHost)
                    .subscribe(subscribeMessage, serverResponseStream);
            ServiceMetrics.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                    RcaGraphMetrics.NET_BYTES_OUT,
                    subscribeMessage.getRequesterGraphNode(),
                    subscribeMessage.getSerializedSize());
        } catch (StatusRuntimeException sre) {
            LOG.error("Encountered an error trying to subscribe. Status: {}", sre.getStatus(), sre);
            StatsCollector.instance().logException(StatExceptionCode.RCA_NETWORK_ERROR);
        }
    }

    /**
     * Gets a stream from the remote host to write flow units to. If there are failures while
     * writing to the stream, the subscribers will fail and trigger a new subscription which
     * re-establishes the stream.
     *
     * @param remoteHost The remote host to which we need to send flow units to.
     * @param flowUnitMessage The flow unit to send to the remote host.
     * @param serverResponseStream The stream for the server to communicate back on.
     */
    public void publish(
            final InstanceDetails remoteHost,
            final FlowUnitMessage flowUnitMessage,
            final StreamObserver<PublishResponse> serverResponseStream) {
        LOG.debug("Publishing {} data to {}", flowUnitMessage.getGraphNode(), remoteHost);
        try {
            final StreamObserver<FlowUnitMessage> stream =
                    getDataStreamForHost(
                            remoteHost, flowUnitMessage.getGraphNode(), serverResponseStream);
            stream.onNext(flowUnitMessage);
            ServiceMetrics.RCA_GRAPH_METRICS_AGGREGATOR.updateStat(
                    RcaGraphMetrics.NET_BYTES_OUT,
                    flowUnitMessage.getGraphNode(),
                    flowUnitMessage.getSerializedSize());
        } catch (StatusRuntimeException sre) {
            LOG.error(
                    "rca: Encountered an error trying to publish a flow unit. Status: {}",
                    sre.getStatus(),
                    sre);
            StatsCollector.instance().logException(StatExceptionCode.RCA_NETWORK_ERROR);
        }
    }

    public void getMetrics(
            InstanceDetails remoteNodeIP,
            MetricsRequest request,
            StreamObserver<MetricsResponse> responseObserver) {
        InterNodeRpcServiceGrpc.InterNodeRpcServiceStub stub =
                connectionManager.getClientStubForHost(remoteNodeIP);
        stub.getMetrics(request, responseObserver);
    }

    public void stop() {
        LOG.debug("Shutting down client streaming connections..");
        closeAllDataStreams();
        this.connectionManager.shutdown();
    }

    public void flushStream(final InstanceDetails.Id remoteHost) {
        LOG.debug("removing data streams for {} as we are no publishing to it.", remoteHost);
        perHostAndNodeOpenDataStreamMap.remove(remoteHost);
    }

    private void closeAllDataStreams() {
        for (Map.Entry<
                        InstanceDetails.Id,
                        ConcurrentMap<String, AtomicReference<StreamObserver<FlowUnitMessage>>>>
                entry : perHostAndNodeOpenDataStreamMap.entrySet()) {
            LOG.debug("Closing stream for host: {}", entry.getKey());
            // Sending an onCompleted should trigger the subscriber's node state manager
            // and cause this host to be put under observation.
            // Closes stream for each node for an instance.
            for (Map.Entry<String, AtomicReference<StreamObserver<FlowUnitMessage>>>
                    perInstanceEntry : entry.getValue().entrySet()) {
                perInstanceEntry.getValue().get().onCompleted();
            }
            perHostAndNodeOpenDataStreamMap.remove(entry.getKey());
        }
    }

    private StreamObserver<FlowUnitMessage> getDataStreamForHost(
            final InstanceDetails remoteHost,
            final String graphNode,
            final StreamObserver<PublishResponse> serverResponseStream) {
        final ConcurrentMap<String, AtomicReference<StreamObserver<FlowUnitMessage>>>
                streamObserverAtomicReference =
                        perHostAndNodeOpenDataStreamMap.get(remoteHost.getInstanceId());
        if (streamObserverAtomicReference != null
                && streamObserverAtomicReference.get(graphNode) != null) {
            return streamObserverAtomicReference.get(graphNode).get();
        }
        return addOrUpdateDataStreamForHost(remoteHost, graphNode, serverResponseStream);
    }

    /**
     * Builds or updates a flow unit data stream to a host. Callers: Send data thread.
     *
     * @param remoteHost The host to which we want to open a stream to.
     * @param serverResponseStream The response stream object.
     * @return A stream to the host.
     */
    private synchronized StreamObserver<FlowUnitMessage> addOrUpdateDataStreamForHost(
            final InstanceDetails remoteHost,
            final String graphNode,
            final StreamObserver<PublishResponse> serverResponseStream) {
        InterNodeRpcServiceGrpc.InterNodeRpcServiceStub stub =
                connectionManager.getClientStubForHost(remoteHost);
        final StreamObserver<FlowUnitMessage> dataStream = stub.publish(serverResponseStream);
        perHostAndNodeOpenDataStreamMap.computeIfAbsent(
                remoteHost.getInstanceId(),
                k ->
                        new ConcurrentHashMap<
                                String, AtomicReference<StreamObserver<FlowUnitMessage>>>() {
                            {
                                put(graphNode, new AtomicReference<>());
                            }
                        });
        perHostAndNodeOpenDataStreamMap
                .get(remoteHost.getInstanceId())
                .computeIfAbsent(graphNode, k -> new AtomicReference<>())
                .set(dataStream);
        return dataStream;
    }
}
