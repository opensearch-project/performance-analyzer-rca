/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 *
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.opensearch.performanceanalyzer;


import com.sun.net.httpserver.HttpServer;
import org.opensearch.performanceanalyzer.net.NetClient;
import org.opensearch.performanceanalyzer.net.NetServer;

/** A wrapper class to return all the server created by the App. */
public class ClientServers {
    /** Http server responds to the curl requests. */
    private final HttpServer httpServer;

    /** The net server is the gRPC server. All inter-node communication is gRPC based. */
    private final NetServer netServer;

    /** Client to make gRPC requests. */
    private final NetClient netClient;

    public ClientServers(HttpServer httpServer, NetServer netServer, NetClient netClient) {
        this.httpServer = httpServer;
        this.netServer = netServer;
        this.netClient = netClient;
    }

    public HttpServer getHttpServer() {
        return httpServer;
    }

    public NetServer getNetServer() {
        return netServer;
    }

    public NetClient getNetClient() {
        return netClient;
    }
}
