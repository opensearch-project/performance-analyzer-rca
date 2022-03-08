/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.contexts;


import java.util.ArrayList;
import java.util.List;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.opensearch.performanceanalyzer.grpc.ResourceContextMessage;
import org.opensearch.performanceanalyzer.rca.framework.api.Resources;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericContext;

/** context that goes along with ResourceFlowUnit. */
public class ResourceContext extends GenericContext {
    public ResourceContext(Resources.State state) {
        super(state);
    }

    public static ResourceContext generic() {
        return new ResourceContext(Resources.State.UNKNOWN);
    }

    public ResourceContextMessage buildContextMessage() {
        final ResourceContextMessage.Builder contextMessageBuilder =
                ResourceContextMessage.newBuilder();
        contextMessageBuilder.setState(getState().ordinal());
        return contextMessageBuilder.build();
    }

    public static ResourceContext buildResourceContextFromMessage(ResourceContextMessage message) {
        return new ResourceContext(Resources.State.values()[message.getState()]);
    }

    public List<Field<?>> getSqlSchema() {
        List<Field<?>> schemaList = new ArrayList<>();
        schemaList.add(DSL.field(DSL.name(SQL_SCHEMA_CONSTANTS.STATE_COL_NAME), String.class));
        return schemaList;
    }

    public List<Object> getSqlValue() {
        List<Object> valueList = new ArrayList<>();
        valueList.add(getState().toString());
        return valueList;
    }

    public static class SQL_SCHEMA_CONSTANTS {
        public static final String STATE_COL_NAME = "State";
    }
}
