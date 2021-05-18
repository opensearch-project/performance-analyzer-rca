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
