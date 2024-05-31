/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.net.requests;

import io.grpc.stub.StreamObserver;
import org.opensearch.performanceanalyzer.grpc.SubscribeMessage;
import org.opensearch.performanceanalyzer.grpc.SubscribeResponse;

/** Composite object that encapsulates the subscribe request message and the response stream. */
public class CompositeSubscribeRequest {

    /** The subscribe protobuf message. */
    private final SubscribeMessage subscribeMessage;

    /** The response stream to talk to the client on. */
    private final StreamObserver<SubscribeResponse> subscribeResponseStream;

    public CompositeSubscribeRequest(
            SubscribeMessage subscribeMessage,
            StreamObserver<SubscribeResponse> subscribeResponseStream) {
        this.subscribeMessage = subscribeMessage;
        this.subscribeResponseStream = subscribeResponseStream;
    }

    /**
     * Get the subscribe request.
     *
     * @return The subscribe request protobuf message.
     */
    public SubscribeMessage getSubscribeMessage() {
        return subscribeMessage;
    }

    /**
     * Get the response stream for the request returned by getSubscribeMessage().
     *
     * @return The response stream to write response to for the subscribe request.
     */
    public StreamObserver<SubscribeResponse> getSubscribeResponseStream() {
        return subscribeResponseStream;
    }
}
