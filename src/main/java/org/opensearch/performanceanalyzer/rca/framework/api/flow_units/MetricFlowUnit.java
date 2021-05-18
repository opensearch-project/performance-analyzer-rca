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

package org.opensearch.performanceanalyzer.rca.framework.api.flow_units;


import org.jooq.Record;
import org.jooq.Result;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.util.InstanceDetails;

public class MetricFlowUnit extends GenericFlowUnit {

    private Result<Record> data = null;

    public MetricFlowUnit(long timeStamp) {
        super(timeStamp);
    }

    public MetricFlowUnit(long timeStamp, Result<Record> data) {
        super(timeStamp);
        this.data = data;
        this.empty = false;
    }

    /**
     * read SQL result from flowunit
     *
     * @return SQL result
     */
    public Result<Record> getData() {
        return data;
    }

    public static MetricFlowUnit generic() {
        return new MetricFlowUnit(System.currentTimeMillis());
    }

    /**
     * Metric flowunit is not supposed be serialized and sent over to remote nodes. This function
     * will never be called. so return null in case we run into it.
     */
    @Override
    public FlowUnitMessage buildFlowUnitMessage(
            final String graphNode, final InstanceDetails.Id node) {
        return null;
    }

    @Override
    public String toString() {
        return String.format("%d: %s", this.getTimeStamp(), this.getData());
    }
}
