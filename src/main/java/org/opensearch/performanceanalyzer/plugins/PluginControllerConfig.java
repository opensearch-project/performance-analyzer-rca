/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.plugins;


import java.util.ArrayList;
import java.util.List;

public class PluginControllerConfig {

    private List<Class<? extends Plugin>> frameworkPlugins;

    public PluginControllerConfig() {
        frameworkPlugins = new ArrayList<>();
        frameworkPlugins.add(PublisherEventsLogger.class);
    }

    /** Returns a list of entry point classes for internal framework plugins */
    public List<Class<? extends Plugin>> getFrameworkPlugins() {
        return frameworkPlugins;
    }
}
