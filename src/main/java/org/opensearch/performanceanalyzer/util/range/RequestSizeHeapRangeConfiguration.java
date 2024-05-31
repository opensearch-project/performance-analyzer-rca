/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.util.range;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/** RequestSize controller threshold based on heap occupancy percent range */
public class RequestSizeHeapRangeConfiguration implements RangeConfiguration {

    /**
     * Default request-size controller threshold for lower and upper bound of heap percent Here, new
     * Range(0, 80, 15.0) => for heap percent between 0% and 80% set threshold to 15% Idea here is
     * to incentivize with more buckets to request-size controller if more heap is available. As
     * memory pressure increases we reduce buckets to further reduce number of acceptable incoming
     * requests.
     */
    private final Collection<Range> DEFAULT_RANGE_CONFIGURATION =
            Collections.unmodifiableList(
                    Arrays.asList(
                            new Range(0, 80, 15.0),
                            new Range(81, 90, 10.0),
                            new Range(91, 100, 5.0)));

    private Collection<Range> rangeConfiguration;

    public RequestSizeHeapRangeConfiguration() {
        this.rangeConfiguration = DEFAULT_RANGE_CONFIGURATION;
    }

    @Override
    public Range getRange(double value) {
        return rangeConfiguration.stream()
                .filter(range -> range.contains(value))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean hasRangeChanged(double previousValue, double currentValue) {
        Range previousRange = getRange(previousValue);
        Range currentRange = getRange(currentValue);
        if (previousRange == null || currentRange == null) {
            return false;
        }
        return !previousRange.equals(currentRange);
    }

    @Override
    public void setRangeConfiguration(Collection<Range> rangeConfiguration) {
        this.rangeConfiguration = rangeConfiguration;
    }
}
