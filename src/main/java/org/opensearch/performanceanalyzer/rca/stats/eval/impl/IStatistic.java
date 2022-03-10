/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.performanceanalyzer.rca.stats.eval.impl;


import java.util.Collection;
import org.opensearch.performanceanalyzer.rca.stats.eval.Statistics;
import org.opensearch.performanceanalyzer.rca.stats.eval.impl.vals.Value;

/** This is the template of the statistic classes. Max, min etc. all follow this template. */
public interface IStatistic<V extends Value> {

    /**
     * To obtain the type of statistic such as {@code Statistic.MAX} etc.
     *
     * @return The statistics type it implements.
     */
    Statistics type();

    /**
     * The actual calculation of the metric.
     *
     * @param key How to identify each sample in the measurement. Say, if we are measure the time
     *     spent in the call of the operate() method of the RCA graph and want to find the RCA class
     *     that has the most expensive call. So the key will be the name of the RCA class.
     * @param value The measurement on which statistics are calculated.
     */
    void calculate(String key, Number value);

    /**
     * Get the value of the statistic.
     *
     * @return Get the calculated value.
     */
    Collection<V> get();

    /**
     * To determine if the metric has a valid value to report.
     *
     * @return true if this was ever calculated or else returns false;
     */
    boolean isEmpty();
}
