/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.exceptions;

public class MalformedThresholdFile extends MalformedConfig {
    public MalformedThresholdFile(String fileLocation, String errorMessage) {
        super(fileLocation, errorMessage);
    }
}
