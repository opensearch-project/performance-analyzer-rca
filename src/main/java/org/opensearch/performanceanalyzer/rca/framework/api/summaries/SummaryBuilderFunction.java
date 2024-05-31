/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.summaries;

import org.jooq.Record;

@FunctionalInterface
public interface SummaryBuilderFunction<T> {
    T buildSummary(Record record);
}
