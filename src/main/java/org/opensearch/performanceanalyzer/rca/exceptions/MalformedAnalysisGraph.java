/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.exceptions;

public class MalformedAnalysisGraph extends RuntimeException {
    public MalformedAnalysisGraph(String message) {
        super(message);
    }
}
