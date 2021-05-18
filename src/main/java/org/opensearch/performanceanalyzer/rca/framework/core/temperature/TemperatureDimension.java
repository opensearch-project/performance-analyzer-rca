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
 * Copyright 2020-2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package org.opensearch.performanceanalyzer.rca.framework.core.temperature;


import org.opensearch.performanceanalyzer.rca.framework.api.metrics.ShardSize;

/**
 * This class contains the dimensions over which we are calculating the Temperature profile. We are
 * persisting both the raw metrics and the calculated normalized values for the dimensions defined
 * in this class.
 */
public enum TemperatureDimension {
    CPU_Utilization(
            org.opensearch.performanceanalyzer.rca.framework.api.metrics.CPU_Utilization.NAME),
    Heap_AllocRate(
            org.opensearch.performanceanalyzer.rca.framework.api.metrics.Heap_AllocRate.NAME),
    Shard_Size_In_Bytes(ShardSize.NAME);

    public final String NAME;

    TemperatureDimension(String name) {
        this.NAME = name;
    }
}
