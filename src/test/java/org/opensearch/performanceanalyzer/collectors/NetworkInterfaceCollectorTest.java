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

package org.opensearch.performanceanalyzer.collectors;


import org.junit.Before;

public class NetworkInterfaceCollectorTest extends AbstractCollectorTest {
    @Before
    public void setup() {
        setUut(new NetworkInterfaceCollector());
    }

    @Override
    public void validateMetric(String metric) throws Exception {
        NetInterfaceSummary interfaceSummary = mapper.readValue(metric, NetInterfaceSummary.class);
        // TODO implement further validation of the MetricStatus
        NetInterfaceSummary.Direction direction = interfaceSummary.getDirection();
        double packetRate4 = interfaceSummary.getPacketRate4();
        double dropRate4 = interfaceSummary.getDropRate4();
        double packetRate6 = interfaceSummary.getPacketRate6();
        double dropRate6 = interfaceSummary.getPacketRate6();
        double bps = interfaceSummary.getBps();
    }
}
