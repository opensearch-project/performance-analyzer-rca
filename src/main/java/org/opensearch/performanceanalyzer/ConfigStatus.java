/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer;

public final class ConfigStatus {
    private boolean configMissingOrIncorrect = false;
    public static final ConfigStatus INSTANCE = new ConfigStatus();

    private ConfigStatus() {}

    public boolean haveValidConfig() {
        return !configMissingOrIncorrect;
    }

    public void setConfigurationInvalid() {
        configMissingOrIncorrect = true;
    }
}
