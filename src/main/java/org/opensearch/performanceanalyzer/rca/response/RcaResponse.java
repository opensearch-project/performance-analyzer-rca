/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.response;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.GeneratedMessageV3;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.exception.DataTypeException;
import org.opensearch.performanceanalyzer.grpc.FlowUnitMessage;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.ResourceFlowUnit;
import org.opensearch.performanceanalyzer.rca.framework.api.summaries.HotClusterSummary;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericSummary;

/**
 * RcaResponse contains cluster level info such as cluster state, number of healthy and unhealthy
 * nodes for a particular rca.
 */
public class RcaResponse extends GenericSummary {
    private static final Logger LOG = LogManager.getLogger(RcaResponse.class);
    private String rcaName;
    private String state;
    private long timeStamp;
    List<GenericSummary> summaryList;

    public RcaResponse(String rcaName, String state, long timeStamp) {
        this.rcaName = rcaName;
        this.state = state;
        this.timeStamp = timeStamp;
        this.summaryList = new ArrayList<>();
    }

    public String getRcaName() {
        return rcaName;
    }

    public String getState() {
        return state;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public static RcaResponse buildResponse(Record record) {
        if (record == null) {
            return null;
        }
        RcaResponse response = null;
        try {
            String rcaName =
                    record.get(
                            ResourceFlowUnit.ResourceFlowUnitFieldValue.RCA_NAME_FILELD.getField(),
                            String.class);
            String state =
                    record.get(
                            ResourceFlowUnit.ResourceFlowUnitFieldValue.STATE_NAME_FILELD
                                    .getField(),
                            String.class);
            Long timeStamp =
                    record.get(
                            ResourceFlowUnit.ResourceFlowUnitFieldValue.TIMESTAMP_FIELD.getField(),
                            Long.class);
            if (timeStamp != null) {
                response = new RcaResponse(rcaName, state, timeStamp);
            }
        } catch (IllegalArgumentException ie) {
            LOG.error("Some field is not found in record, cause : {}", ie.getMessage());
        } catch (DataTypeException de) {
            LOG.error("Fails to convert data type");
        }
        return response;
    }

    /**
     * Since RcaResponse Object is a API wrapper for Flowunit & summaries we do not need to support
     * gPRC. Neither will we persist this wrapper.
     */
    @Override
    public GeneratedMessageV3 buildSummaryMessage() {
        return null;
    }

    @Override
    public void buildSummaryMessageAndAddToFlowUnit(FlowUnitMessage.Builder messageBuilder) {}

    @Override
    public String getTableName() {
        return ResourceFlowUnit.RCA_TABLE_NAME;
    }

    @Override
    public List<Field<?>> getSqlSchema() {
        return null;
    }

    @Override
    public List<Object> getSqlValue() {
        return null;
    }

    @Override
    public JsonElement toJson() {
        JsonObject summaryObj = new JsonObject();
        summaryObj.addProperty(ResourceFlowUnit.SQL_SCHEMA_CONSTANTS.RCA_COL_NAME, this.rcaName);
        summaryObj.addProperty(
                ResourceFlowUnit.SQL_SCHEMA_CONSTANTS.TIMESTAMP_COL_NAME, this.timeStamp);
        summaryObj.addProperty(ResourceFlowUnit.SQL_SCHEMA_CONSTANTS.STATE_COL_NAME, this.state);
        if (!getNestedSummaryList().isEmpty()) {
            String tableName = getNestedSummaryList().get(0).getTableName();
            summaryObj.add(tableName, this.nestedSummaryListToJson());
        }
        return summaryObj;
    }

    @Override
    public List<GenericSummary> getNestedSummaryList() {
        return summaryList;
    }

    @Override
    public GenericSummary buildNestedSummary(String summaryTable, Record record) {
        GenericSummary ret = null;
        if (summaryTable.equals(HotClusterSummary.HOT_CLUSTER_SUMMARY_TABLE)) {
            HotClusterSummary hotClusterSummary = HotClusterSummary.buildSummary(record);
            if (hotClusterSummary != null) {
                summaryList.add(hotClusterSummary);
                ret = hotClusterSummary;
            }
        }
        return ret;
    }

    @Override
    public List<String> getNestedSummaryTables() {
        return Collections.unmodifiableList(
                Collections.singletonList(HotClusterSummary.HOT_CLUSTER_SUMMARY_TABLE));
    }

    public void addNestedSummaryList(GenericSummary summary) {
        summaryList.add(summary);
    }
}
