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

package org.opensearch.performanceanalyzer.rca.scheduler;


import org.opensearch.performanceanalyzer.rca.framework.core.Node;
import org.opensearch.performanceanalyzer.rca.framework.core.Queryable;
import org.opensearch.performanceanalyzer.rca.net.WireHopper;
import org.opensearch.performanceanalyzer.rca.persistence.NetPersistor;
import org.opensearch.performanceanalyzer.rca.persistence.Persistable;

public class FlowUnitOperationArgWrapper {
    private final Node<?> node;
    private final Queryable queryable;
    private final Persistable persistable;
    private final WireHopper wireHopper;
    private final NetPersistor netPersistor;

    public Node<?> getNode() {
        return node;
    }

    public Queryable getQueryable() {
        return queryable;
    }

    public Persistable getPersistable() {
        return persistable;
    }

    public WireHopper getWireHopper() {
        return wireHopper;
    }

    FlowUnitOperationArgWrapper(
            Node<?> node, Queryable queryable, Persistable persistable, WireHopper wireHopper) {
        this.node = node;
        this.queryable = queryable;
        this.persistable = persistable;
        this.wireHopper = wireHopper;
        this.netPersistor = null;
    }
}
