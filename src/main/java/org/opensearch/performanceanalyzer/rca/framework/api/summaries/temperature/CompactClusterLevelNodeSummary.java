/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.summaries.temperature;

import static org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary.SQL_SCHEMA_CONSTANTS.HOST_IP_ADDRESS_COL_NAME;
import static org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotNodeSummary.SQL_SCHEMA_CONSTANTS.NODE_ID_COL_NAME;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Record;
import org.jooq.exception.DataTypeException;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureDimension;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureVector;

/**
 * This object is instantiated on the elected cluster_manager. This is the cluster_manager's view of
 * the {@code CompactNodeSummary}. The difference is that, the CompactNodeSummary is based on the
 * node level resource utilization by the shards and the {@code CompactClusterLevelNodeSummary} is
 * based on the utilization across multiple nodes in the cluster.
 */
public class CompactClusterLevelNodeSummary extends CompactNodeSummary {
    private static final Logger LOG = LogManager.getLogger(CompactClusterLevelNodeSummary.class);

    public final String TABLE_NAME = CompactClusterLevelNodeSummary.class.getSimpleName();

    public CompactClusterLevelNodeSummary(String nodeId, String hostAddress) {
        super(nodeId, hostAddress);
    }

    /**
     * CompactClusterLevelNodeSummary_ID|node_id|host_address|CPU_Utilization
     * |CPU_Utilization_total|..
     *
     * @param record A database record
     * @return Summary object constructed from the database row
     */
    public static CompactClusterLevelNodeSummary build(Record record) {
        String nodeId = record.get(NODE_ID_COL_NAME, String.class);
        String hostIp = record.get(HOST_IP_ADDRESS_COL_NAME, String.class);

        CompactClusterLevelNodeSummary summary = new CompactClusterLevelNodeSummary(nodeId, hostIp);

        for (TemperatureDimension dimension : TemperatureDimension.values()) {
            try {
                Short mean = record.get(dimension.NAME + MEAN_SUFFIX_KEY, Short.class);
                double total = record.get(dimension.NAME + TOTAL_SUFFIX_KEY, Double.class);
                int num_shards = record.get(dimension.NAME + NUM_SHARDS_SUFFIX_KEY, Integer.class);

                summary.setNumOfShards(dimension, num_shards);
                summary.setTemperatureForDimension(
                        dimension, new TemperatureVector.NormalizedValue(mean));
                summary.setTotalConsumedByDimension(dimension, total);
            } catch (DataTypeException dex) {
                LOG.error("Could not create valid summary object.", dex);
            }
        }
        return summary;
    }
}
