/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.integTests.framework.runners;

public class RcaItNotEncryptedRunner extends RcaItRunnerBase {
    private static final boolean USE_HTTPS = false;

    public RcaItNotEncryptedRunner(Class testClass) throws Exception {
        super(testClass, USE_HTTPS);
    }
}
