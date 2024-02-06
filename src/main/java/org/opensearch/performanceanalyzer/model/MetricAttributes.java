/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.model;

import java.util.HashSet;
import org.opensearch.performanceanalyzer.commons.metrics.MetricDimension;

public class MetricAttributes {
    public String unit;
    public HashSet<String> dimensionNames;

    MetricAttributes(String unit, MetricDimension[] dimensions) {

        this.unit = unit;
        this.dimensionNames = new HashSet<String>();
        for (MetricDimension dimension : dimensions) {
            this.dimensionNames.add(dimension.toString());
        }
    }
}
