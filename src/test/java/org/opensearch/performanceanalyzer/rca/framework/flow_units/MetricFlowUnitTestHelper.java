/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.flow_units;

import java.util.ArrayList;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.Mock;
import org.jooq.tools.jdbc.MockConnection;
import org.opensearch.performanceanalyzer.rca.framework.api.flow_units.MetricFlowUnit;

public class MetricFlowUnitTestHelper {

    public static MetricFlowUnit createFlowUnit(
            final List<String> fieldName, final List<String>... rows) {
        DSLContext context = DSL.using(new MockConnection(Mock.of(0)));
        List<String[]> stringData = new ArrayList<>();
        stringData.add(fieldName.toArray(new String[0]));
        for (int i = 0; i < rows.length; i++) {
            stringData.add(rows[i].toArray(new String[0]));
        }
        Result<Record> newRecordList = context.fetchFromStringData(stringData);
        return new MetricFlowUnit(0, newRecordList);
    }
}
