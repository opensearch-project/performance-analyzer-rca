/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.api.persist;


import org.jooq.Field;

/**
 * This interface helps writing enums for the field in each RCA table (FlowUnit, summaries etc.) We
 * can call this getField method to read the field object directly without worrying about casting
 * the field's name and data type.
 */
public interface JooqFieldValue {
    String getName();

    Field getField();
}
