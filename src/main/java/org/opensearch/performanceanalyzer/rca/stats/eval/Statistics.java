/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.stats.eval;

/** List of stats that are currently supported. */
public enum Statistics {
    MAX,
    MIN,
    MEAN,
    COUNT,
    SUM,

    // Samples are not aggregated. They are reported as they were found. They can be used when we
    // need just a key value pairs.
    SAMPLE,

    // Think of them as a counter per name. So if you update your stats as these values:
    // x, y, x, x, z, h
    // then the named counter will give you something like:
    // x: 3, y: 1, z: 1, h:1
    // This is helpful in calculating metric like which rca nodes threw exceptions and count per
    // graph node.
    NAMED_COUNTERS
}
