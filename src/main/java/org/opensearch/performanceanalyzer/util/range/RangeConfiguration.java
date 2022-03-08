/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.util.range;


import java.util.Collection;

public interface RangeConfiguration {
    Range getRange(double value);

    boolean hasRangeChanged(double previousValue, double currentValue);

    void setRangeConfiguration(Collection<Range> rangeConfiguration);
}
