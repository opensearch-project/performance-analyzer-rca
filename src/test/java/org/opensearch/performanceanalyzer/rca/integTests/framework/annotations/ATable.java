/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.framework.annotations;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.opensearch.performanceanalyzer.rca.integTests.framework.configs.HostTag;

/**
 * This specifies a table for a given metric. This annotation is a sub-field of the AMetric
 * annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ATable {
    // Which host should emit this metric
    HostTag[] hostTag();

    // The data in tabular form.
    ATuple[] tuple();
}
