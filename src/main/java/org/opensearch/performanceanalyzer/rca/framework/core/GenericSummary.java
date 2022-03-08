/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.core;


import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.protobuf.GeneratedMessageV3;
import java.util.ArrayList;
import java.util.List;
import org.jooq.Field;
import org.jooq.Record;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;

public abstract class GenericSummary {

    public abstract <T extends GeneratedMessageV3> T buildSummaryMessage();

    public abstract void buildSummaryMessageAndAddToFlowUnit(
            FlowUnitMessage.Builder messageBuilder);

    // get the name of SQL table for each summary
    public abstract String getTableName();

    public abstract List<Field<?>> getSqlSchema();

    public abstract List<Object> getSqlValue();

    /** convert the summary object to Gson object */
    public abstract JsonElement toJson();

    /**
     * get the list of nested summary in generic type. Used by persistor only
     *
     * @return list of Genericsummary
     */
    public List<GenericSummary> getNestedSummaryList() {
        return new ArrayList<>();
    }

    /**
     * get the list of SQL tables that store the nested summaries this summary contains each summary
     * can be attached with one or many types of nested summary. So we use this to recursively query
     * nested summary table in SQL. Used by persistor only
     *
     * @return list of SQL tables
     */
    public List<String> getNestedSummaryTables() {
        return null;
    }

    /**
     * build nested summary from SQL record and append it to the corresponding nested summary list
     * Used by persistor only
     *
     * @param summaryTable the SQL table of the nested summary
     * @param record the SQL record of nested summary
     * @return the nested summary of generic type
     */
    public GenericSummary buildNestedSummary(String summaryTable, Record record)
            throws IllegalArgumentException {
        return null;
    }

    /**
     * convert summary list into a json array
     *
     * @return JsonArray object
     */
    @VisibleForTesting
    public JsonArray nestedSummaryListToJson() {
        JsonArray nestedSummaryArray = new JsonArray();
        if (getNestedSummaryList() != null) {
            getNestedSummaryList()
                    .forEach(
                            summary -> {
                                nestedSummaryArray.add(summary.toJson());
                            });
        }
        return nestedSummaryArray;
    }
}
