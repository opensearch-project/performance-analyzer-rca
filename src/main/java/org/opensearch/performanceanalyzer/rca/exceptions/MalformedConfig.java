/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.exceptions;

public class MalformedConfig extends Exception {
    public MalformedConfig(String fileLocation, String errorMessage) {
        super(String.format("Malformed config file: (%s). Err: %s", fileLocation, errorMessage));
    }
}
