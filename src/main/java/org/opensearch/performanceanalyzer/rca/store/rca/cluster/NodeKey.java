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
 * Copyright 2019-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package org.opensearch.performanceanalyzer.rca.store.rca.cluster;


import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;

public class NodeKey {
    private final InstanceDetails.Id nodeId;
    private final InstanceDetails.Ip hostAddress;

    public NodeKey(InstanceDetails.Id nodeId, InstanceDetails.Ip hostAddress) {
        this.nodeId = nodeId;
        this.hostAddress = hostAddress;
    }

    public NodeKey(InstanceDetails instanceDetails) {
        this(instanceDetails.getInstanceId(), instanceDetails.getInstanceIp());
    }

    public InstanceDetails.Id getNodeId() {
        return nodeId;
    }

    public InstanceDetails.Ip getHostAddress() {
        return hostAddress;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NodeKey) {
            NodeKey key = (NodeKey) obj;
            return nodeId.equals(key.getNodeId()) && hostAddress.equals(key.getHostAddress());
        }
        return false;
    }

    // the reason why we compare both node id and  hostAddress here is because in
    // newer version (6.8 and above), see https://github.com/elastic/elasticsearch/pull/19140.
    // if opensearch restart, both node id and ip address will remain the same so we can continue
    // add
    // flowunit into the same row in table before opensearch restart.
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(nodeId).append(hostAddress).toHashCode();
    }

    @Override
    public String toString() {
        return nodeId + " " + hostAddress;
    }
}
