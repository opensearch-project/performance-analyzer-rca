/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.core;

public interface Gatherable<T> {
    T gather(Queryable queryable);
}
