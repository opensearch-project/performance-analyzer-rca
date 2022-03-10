/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.exceptions;

public class OverridesAndPrecedenceOrderCountMismatch extends MalformedThresholdFile {
    public OverridesAndPrecedenceOrderCountMismatch(String fileLocation, String errorMessage) {
        super(fileLocation, errorMessage);
    }
}
