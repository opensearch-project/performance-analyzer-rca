/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.summaries.temperature;

import com.google.gson.JsonObject;
import com.google.protobuf.GeneratedMessageV3;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureDimension;
import org.opensearch.performanceanalyzer.rca.framework.core.temperature.TemperatureVector;

public class ShardProfileSummary extends GenericSummary {

    public static final String SUMMARY_TABLE_NAME = "ShardProfileSummary";
    public static final String INDEX_NAME_KEY = "index_name";
    public static final String SHARD_ID_KEY = "shard_id";
    public static final String TEMPERATURE_KEY = "temperature";

    private final String indexName;
    private final int shardId;

    private final TemperatureVector temperatureVector;

    public ShardProfileSummary(String indexName, int shardId) {
        this.indexName = indexName;
        this.shardId = shardId;
        this.temperatureVector = new TemperatureVector();
    }

    public String identity() {
        return indexName + "::" + shardId;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    @Override
    public <T extends GeneratedMessageV3> T buildSummaryMessage() {
        throw new IllegalArgumentException();
    }

    @Override
    public void buildSummaryMessageAndAddToFlowUnit(FlowUnitMessage.Builder messageBuilder) {
        throw new IllegalArgumentException();
    }

    @Override
    public String getTableName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public List<Field<?>> getSqlSchema() {
        List<Field<?>> schema = new ArrayList<>();
        schema.add(DSL.field(DSL.name(INDEX_NAME_KEY), String.class));
        schema.add(DSL.field(DSL.name(SHARD_ID_KEY), Integer.class));
        for (TemperatureDimension dimension : TemperatureDimension.values()) {
            schema.add(DSL.field(DSL.name(dimension.NAME), Short.class));
        }
        return schema;
    }

    @Override
    public List<Object> getSqlValue() {
        List<Object> values = new ArrayList<>();
        values.add(indexName);
        values.add(shardId);
        for (TemperatureDimension dimension : TemperatureDimension.values()) {
            values.add(temperatureVector.getTemperatureFor(dimension));
        }
        return values;
    }

    public JsonObject toJson() {
        JsonObject summaryObj = new JsonObject();
        summaryObj.addProperty(INDEX_NAME_KEY, indexName);
        summaryObj.addProperty(SHARD_ID_KEY, shardId);
        summaryObj.add(TEMPERATURE_KEY, temperatureVector.toJson());
        return summaryObj;
    }

    @Nullable
    public TemperatureVector.NormalizedValue getHeatInDimension(TemperatureDimension dimension) {
        return temperatureVector.getTemperatureFor(dimension);
    }

    public void addTemperatureForDimension(
            TemperatureDimension dimension, TemperatureVector.NormalizedValue value) {
        // TODO: Need to handle rcas updating heat profile of a shard along a dimension multiple
        //  times per tick.
        temperatureVector.updateTemperatureForDimension(dimension, value);
    }
}
