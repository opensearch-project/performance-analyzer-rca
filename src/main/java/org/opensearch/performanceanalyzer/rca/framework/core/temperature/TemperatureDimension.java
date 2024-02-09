/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
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
