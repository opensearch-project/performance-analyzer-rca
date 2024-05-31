/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
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
