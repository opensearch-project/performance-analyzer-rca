/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.decisionmaker.actions.configs;

public interface ThresholdConfig<T> {

    T upperBound();

    T lowerBound();
}
