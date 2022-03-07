/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.plugins;

/**
 * Allows adding custom extensions to the analysis graph.
 *
 * <p>RCA framework plugins can be installed to extend the analysis graph through custom metric
 * nodes, rca nodes, deciders or action listeners. These can subscribe to flow units from existing
 * nodes to add new functionality, or override existing graph nodes to customize for specific use
 * cases.
 */
public abstract class Plugin {

    public abstract String name();
}
