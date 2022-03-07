/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.reader;

/** Interface that should be implemented by snapshot holders that need to be trimmed. */
public interface Removable {
    void remove() throws Exception;
}
