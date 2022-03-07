/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.framework.core;

/*
 * In order to operate an Operable, the input multiple streams and the output is one single stream. Each of the
 * dependencies will have multiple samples and there are expected to be multiple dependencies. Hence, the input type
 * is list of lists.
 */
public interface Operable<T> {
    T operate();
}
