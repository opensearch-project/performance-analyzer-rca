/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
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
