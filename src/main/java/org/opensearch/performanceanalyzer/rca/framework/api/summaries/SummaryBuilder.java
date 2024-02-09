/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.summaries;

import org.jooq.Record;
import org.opensearch.performanceanalyzer.rca.framework.core.GenericSummary;

public class SummaryBuilder<T extends GenericSummary> {
    private final String tableName;
    SummaryBuilderFunction<T> builder;

    public SummaryBuilder(final String tableName, SummaryBuilderFunction<T> builder) {
        this.tableName = tableName;
        this.builder = builder;
    }

    public GenericSummary buildSummary(Record record) {
        return builder.buildSummary(record);
    }

    public String getTableName() {
        return tableName;
    }
}
