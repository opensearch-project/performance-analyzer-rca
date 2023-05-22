/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.store.rca.hotshard;


import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Record;
import org.jooq.exception.DataTypeException;
import org.opensearch.performanceanalyzer.commons.metrics.AllMetrics;

public class IndexShardKey {

    private static final Logger LOG = LogManager.getLogger(IndexShardKey.class);
    private final String indexName;
    private final int shardId;

    public IndexShardKey(String indexName, int shardId) {
        this.indexName = indexName;
        this.shardId = shardId;
    }

    public String getIndexName() {
        return this.indexName;
    }

    public int getShardId() {
        return this.shardId;
    }

    public static IndexShardKey buildIndexShardKey(Record record) throws IllegalArgumentException {
        if (record == null) {
            throw new IllegalArgumentException("record is null");
        }
        try {
            String indexName =
                    record.getValue(AllMetrics.CommonDimension.INDEX_NAME.toString(), String.class);
            Integer shardId =
                    record.getValue(AllMetrics.CommonDimension.SHARD_ID.toString(), Integer.class);
            return new IndexShardKey(indexName, shardId);
        } catch (DataTypeException de) {
            LOG.error("Fail to read field from SQL record, message {}", de.getMessage());
            throw new IllegalArgumentException("failed to read field from record");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof IndexShardKey) {
            IndexShardKey key = (IndexShardKey) obj;
            return indexName.equals(key.indexName) && shardId == key.shardId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(indexName).append(shardId).toHashCode();
    }

    @Override
    public String toString() {
        return "[" + this.indexName + "][" + this.shardId + "]";
    }
}
