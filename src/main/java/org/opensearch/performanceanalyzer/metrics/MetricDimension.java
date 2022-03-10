/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.metrics;

/**
 * This helps writing a general parser. Given a MetricDimension, I can parse the metric file using
 * the values provided by the MetricDimension enum. I don't need to hardcode the exact enum name in
 * the parser. The parser only needs to know this enum has a metric's dimensions and use its members
 * as Json key to parse out the concrete metric dimensions. See
 * src/main/java/org/opensearch/performanceanalyzer/reader/MetricProperties.java
 */
public interface MetricDimension {}
